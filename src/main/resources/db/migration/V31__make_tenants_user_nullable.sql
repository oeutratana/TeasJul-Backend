-- Allow a tenant profile to exist without a linked login account.
-- Business rule: a TENANT is a rental customer/profile and does NOT necessarily
-- have a USER login account. The user-to-tenant link is optional.
--
-- The unique key uk_tenants_user_id is intentionally kept: a user account may
-- still be linked to at most one tenant profile, and NULL rows never collide.

ALTER TABLE tenants
    MODIFY user_id BIGINT UNSIGNED NULL;
