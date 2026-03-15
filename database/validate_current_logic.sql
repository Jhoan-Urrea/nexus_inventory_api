-- validate_current_logic.sql
-- Script de validación para revisar la lógica del modelado actual implementado
-- 1) Revisa que las tablas principales existan
\dt

-- 2) Revisa que existan los roles de negocio definidos en código
SELECT * FROM role WHERE name IN ('ADMIN', 'WAREHOUSE_EMPLOYEE', 'WAREHOUSE_SUPERVISOR', 'USER', 'CLIENT', 'SALES_AGENT');

-- 3) Revisa usuarios y roles asignados (login / auth)
SELECT u.id, u.email, u.username, u.status, r.name AS role_name
FROM app_user u
JOIN user_role ur ON ur.user_id = u.id
JOIN role r ON r.id = ur.role_id
ORDER BY u.id, r.name;

-- 4) Verifica la existencia de tokens para refresh y revocación
SELECT * FROM auth_refresh_token ORDER BY created_at DESC LIMIT 20;
SELECT * FROM auth_revoked_token ORDER BY created_at DESC LIMIT 20;

-- 5) Verifica la tabla de restablecimiento de contraseña, tokens y flags
SELECT * FROM auth_password_reset_token ORDER BY created_at DESC LIMIT 20;

-- 6) Revisa auditoría de eventos de auth
SELECT * FROM auth_audit_log ORDER BY created_at DESC LIMIT 50;

-- 7) Revisa si hay usuarios sin ciudad (debe ser rechazado por FK no nulo)
SELECT COUNT(*) AS missing_city FROM app_user WHERE city_id IS NULL;

-- 8) Revisa foreign key constraints relevantes para seguridad (user-role, role-permission)
SELECT conname, conrelid::regclass AS tabla, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid::regclass::text IN ('user_role', 'role_permission', 'app_user', 'auth_password_reset_token');

-- 9) Lógica de password reset token activo (no vencido y no usado)
SELECT * FROM auth_password_reset_token
WHERE used = FALSE AND expires_at > NOW()
ORDER BY expires_at ASC;

-- 10) Para verificar la lógica de rol por defecto de registro (WAREHOUSE_EMPLOYEE) 
-- esto se valida en la app, pero aquí puedes inspeccionar la tabla de seed y los usuarios creados.
SELECT * FROM role WHERE name = 'WAREHOUSE_EMPLOYEE';
SELECT u.id, u.email, r.name
FROM app_user u
JOIN user_role ur ON ur.user_id = u.id
JOIN role r ON r.id = ur.role_id
WHERE r.name = 'WAREHOUSE_EMPLOYEE'
ORDER BY u.id;

-- 11) Revisa la implementacion de usuarios con roles de supervisor
SELECT u.id, u.email, r.name
FROM app_user u
JOIN user_role ur ON ur.user_id = u.id
JOIN role r ON r.id = ur.role_id
WHERE r.name = 'WAREHOUSE_SUPERVISOR';

-- 12) Revisa si hay inconsistencias de roles (role names duplicados inesperados)
SELECT name, COUNT(*)
FROM role
GROUP BY name
HAVING COUNT(*) > 1;

-- 13) Revisa si hay tokens de restablecimiento usados con expire check
SELECT * FROM auth_password_reset_token
WHERE used = TRUE OR expires_at <= NOW()
ORDER BY created_at DESC LIMIT 20;

-- 14) Para confirmar la existencia de la tabla de permisos en el modelo:
SELECT * FROM permission LIMIT 20;
SELECT * FROM role_permission LIMIT 20;

-- 15) Revisa la configuración de ciudades/warehouses para el alcance de inventario
SELECT COUNT(*) AS warehouses FROM warehouse;
SELECT w.id, w.name, w.active, c.city_name
FROM warehouse w
JOIN city c ON c.city_id = w.city_id
LIMIT 20;
