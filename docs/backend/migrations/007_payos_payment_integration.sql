-- Migration: PayOS Payment Gateway Integration
-- Description: Creates payment_orders table for tracking payments via PayOS
-- Date: 2025-12-13

-- ===========================================
-- PAYMENT ORDERS TABLE
-- ===========================================
-- Tracks all payment attempts for subscriptions and Lúa purchases

CREATE TABLE IF NOT EXISTS public.payment_orders (
    id BIGSERIAL PRIMARY KEY,
    
    -- User reference
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    
    -- PayOS order tracking
    order_code BIGINT NOT NULL UNIQUE,  -- PayOS unique order code (max 9007199254740991)
    payment_link_id VARCHAR(255),        -- PayOS payment link ID
    checkout_url VARCHAR(500),           -- Payment checkout URL
    qr_code TEXT,                        -- QR code data (can be large)
    
    -- Payment type and details
    type VARCHAR(20) NOT NULL CHECK (type IN ('SUBSCRIPTION', 'LUA_PACK')),
    tier_id BIGINT REFERENCES public.subscription_tiers(id),  -- For SUBSCRIPTION type
    tier_code VARCHAR(50),               -- Subscription tier code for display
    lua_amount INTEGER,                  -- For LUA_PACK type
    amount_vnd INTEGER NOT NULL,         -- Payment amount in VND
    description VARCHAR(25),             -- Short description (max 25 chars for banks)
    
    -- Payment status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED', 'FAILED')),
    transaction_datetime VARCHAR(50),    -- PayOS transaction datetime
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    
    -- Constraints
    CONSTRAINT chk_subscription_has_tier 
        CHECK (type != 'SUBSCRIPTION' OR tier_id IS NOT NULL),
    CONSTRAINT chk_lua_has_amount 
        CHECK (type != 'LUA_PACK' OR lua_amount IS NOT NULL)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_id 
    ON public.payment_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_order_code 
    ON public.payment_orders(order_code);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status 
    ON public.payment_orders(status);
CREATE INDEX IF NOT EXISTS idx_payment_orders_created_at 
    ON public.payment_orders(created_at DESC);

-- ===========================================
-- ROW LEVEL SECURITY
-- ===========================================

ALTER TABLE public.payment_orders ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own payment orders
CREATE POLICY payment_orders_select_own ON public.payment_orders
    FOR SELECT
    USING (auth.uid() = user_id);

-- Policy: Users can insert their own payment orders
CREATE POLICY payment_orders_insert_own ON public.payment_orders
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Policy: Service role can do everything (for backend)
CREATE POLICY payment_orders_service_all ON public.payment_orders
    FOR ALL
    USING (auth.role() = 'service_role')
    WITH CHECK (auth.role() = 'service_role');

-- Policy: Authenticated users can update their pending orders only
CREATE POLICY payment_orders_update_own ON public.payment_orders
    FOR UPDATE
    USING (auth.uid() = user_id AND status = 'PENDING')
    WITH CHECK (auth.uid() = user_id);

-- ===========================================
-- HELPER FUNCTION: Check if payment exists
-- ===========================================

CREATE OR REPLACE FUNCTION public.check_pending_payment(
    p_user_id UUID,
    p_type VARCHAR,
    p_tier_id INTEGER DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.payment_orders
        WHERE user_id = p_user_id
          AND type = p_type
          AND status = 'PENDING'
          AND (p_tier_id IS NULL OR tier_id = p_tier_id)
          AND (expires_at IS NULL OR expires_at > NOW())
    );
END;
$$;

-- ===========================================
-- COMMENTS
-- ===========================================

COMMENT ON TABLE public.payment_orders IS 'Tracks all payment attempts via PayOS payment gateway';
COMMENT ON COLUMN public.payment_orders.order_code IS 'PayOS unique order code (required, max 9007199254740991)';
COMMENT ON COLUMN public.payment_orders.payment_link_id IS 'PayOS payment link ID returned after creation';
COMMENT ON COLUMN public.payment_orders.checkout_url IS 'URL for user to complete payment';
COMMENT ON COLUMN public.payment_orders.type IS 'SUBSCRIPTION for tier upgrades, LUA_PACK for Lúa purchases';
COMMENT ON COLUMN public.payment_orders.description IS 'Max 25 chars for bank compatibility';
COMMENT ON COLUMN public.payment_orders.transaction_datetime IS 'PayOS transaction datetime when payment was made';
