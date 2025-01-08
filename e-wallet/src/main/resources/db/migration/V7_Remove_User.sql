DO $$ 
DECLARE 
    x INTEGER := 1; 
BEGIN 
    DELETE FROM transactions WHERE id_sender = x OR id_receiver = x;
    DELETE FROM users WHERE id = x;
END $$;
