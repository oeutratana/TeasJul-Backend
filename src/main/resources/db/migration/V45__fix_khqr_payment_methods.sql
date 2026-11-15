-- KHQR online payments were previously attributed to whatever payment method was
-- "first active" for the owner (often CASH), because the tenant KHQR flow sent no
-- paymentMethodId and the fallback picked FindFirstByOwnerIdAndStatus.
--
-- Only PayWay/KHQR-created payments carry a qr_code, so use it to relabel those
-- records onto the owner's KHQR payment method (creating a default KHQR method
-- when the owner has none).

-- 1) Create a default KHQR payment method for owners who have KHQR payments but
--    no payment method with code = 'KHQR'.
INSERT INTO payment_methods (owner_id, name, code, description, status, created_at, updated_at)
SELECT DISTINCT p.owner_id, 'PayWay KHQR', 'KHQR', 'PayWay Sandbox KHQR Payment', 'ACTIVE', NOW(), NOW()
FROM payments p
WHERE p.qr_code IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM payment_methods existing
      WHERE existing.owner_id = p.owner_id
        AND existing.code = 'KHQR'
  );

-- 2) Point each KHQR payment at the owner's KHQR method.
UPDATE payments p
JOIN payment_methods khqr
    ON khqr.owner_id = p.owner_id
   AND khqr.code = 'KHQR'
JOIN payment_methods current_pm
    ON current_pm.id = p.payment_method_id
SET p.payment_method_id = khqr.id
WHERE p.qr_code IS NOT NULL
  AND current_pm.code <> 'KHQR';