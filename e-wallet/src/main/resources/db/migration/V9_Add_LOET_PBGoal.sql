-- Add lockoutendtime and piggybankgoal columns to the users table

ALTER TABLE users
ADD COLUMN lockoutendtime TIMESTAMP,
ADD COLUMN piggybankgoal DOUBLE PRECISION DEFAULT 0.0;