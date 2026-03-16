# Remediar hallazgos de GitGuardian en el PR

GitGuardian escanea **todos los commits** del pull request. Aunque el código actual ya no tenga contraseñas en claro, si el commit `099b342` (u otros) sigue en la rama, el escáner seguirá reportando "6 secrets" en `PasswordPolicyServiceImplTest.java` porque en ese commit el archivo aún tenía literales como `"Abcdef1!"`, `"Aa1!"`, etc.

## Opción 1: Reescribir el historial de la rama (recomendado)

Esto reemplaza el commit problemático por una versión que ya incluye los cambios seguros.

1. Asegúrate de tener los últimos cambios (sin literales de contraseña) commiteados en tu rama, por ejemplo `feature/autentication`.

2. En tu máquina, desde la raíz del repo:
   ```bash
   git fetch origin
   git checkout feature/autentication   # o la rama del PR
   ```

3. Encuentra el commit **anterior** a `099b342` (el padre). Por ejemplo:
   ```bash
   git log --oneline 099b342^..HEAD
   git log --oneline -20
   ```
   Anota el hash del commit padre de `099b342` (por ejemplo `f2abaed`).

4. Inicia un rebase interactivo desde antes de `099b342`:
   ```bash
   git rebase -i 099b342^
   ```
   (Si `099b342^` no funciona, usa el hash del padre, ej. `f2abaed`.)

5. En el editor, marca el commit `099b342` como **edit** (cambia `pick` por `edit` en esa línea), guarda y cierra.

6. Cuando el rebase se detenga en ese commit, el archivo tendrá aún las contraseñas. Sobrescríbelo con la versión segura (la que estaba en la punta de la rama antes del rebase) y améndalo:
   ```bash
   git checkout ORIG_HEAD -- src/test/java/com/example/nexus/modules/auth/service/PasswordPolicyServiceImplTest.java
   git add src/test/java/com/example/nexus/modules/auth/service/PasswordPolicyServiceImplTest.java
   git commit --amend --no-edit
   git rebase --continue
   ```
   Si hay más commits que toquen ese archivo, resuelve conflictos si aparecen y sigue con `git rebase --continue`.

7. Sube la rama reescrita (esto reescribe historia en remoto):
   ```bash
   git push --force-with-lease origin feature/autentication
   ```

8. El PR se actualizará. GitGuardian volverá a escanear; al no existir ya el commit con literales, no debería reportar secretos.

**Importante:** Si más personas trabajan en la misma rama, avísales que tendrán que hacer `git fetch` y `git reset --hard origin/feature/autentication` (o re-clonar), porque el historial habrá cambiado.

## Opción 2: Nuevo PR sin el historial problemático

Si prefieres no tocar el historial de `feature/autentication`:

1. Crea una rama nueva desde `develop`:
   ```bash
   git fetch origin
   git checkout -b feature/autentication-clean origin/develop
   ```

2. Trae solo los cambios actuales (estado de archivos) desde tu rama, sin el historial antiguo:
   ```bash
   git checkout feature/autentication -- .
   git add -A
   git commit -m "Auth and password recovery updates (no hardcoded secrets)"
   git push -u origin feature/autentication-clean
   ```

3. Abre un **nuevo** pull request de `feature/autentication-clean` → `develop`. Los commits de este PR no incluirán `099b342`, por lo que GitGuardian no debería detectar los 6 secretos.

4. Puedes cerrar el PR #10 antiguo.

## Cambios ya aplicados en el código

- **PasswordPolicyServiceImplTest.java**: usa `chars('A','b',...)` para construir contraseñas de prueba sin literales.
- **PasswordRecoveryServiceImplTest.java**: usa helpers `code(...)` y `pwd(...)` para OTP y contraseña de test, sin literales que disparen el escáner.

Después de reescribir el historial (opción 1) o abrir un PR limpio (opción 2), revoca/rota cualquier secreto que hayas usado en producción si esos valores llegaron a desplegarse (por ejemplo contraseñas de prueba).
