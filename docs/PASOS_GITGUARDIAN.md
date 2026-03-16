# Pasos para que GitGuardian deje de marcar el PR

Sigue estos pasos **en orden** desde la raíz del proyecto.

---

## Paso 1: Guardar los cambios pendientes en un commit

Tienes cambios sin commitear. Guárdalos en un commit:

```bash
git add src/test/java/com/example/nexus/modules/auth/service/PasswordRecoveryServiceImplTest.java
git add docs/
git commit -m "Evitar literales tipo contraseña en tests (GitGuardian)"
```

---

## Paso 2: Iniciar el rebase interactivo

El commit que GitGuardian marca es `099b342`. Su commit padre es `f2abaed`. Vamos a rebasar desde ahí:

```bash
git rebase -i f2abaed
```

Se abrirá un editor con varias líneas. Verás algo como:

```
pick 099b342 Resolve merge conflict keeping feature/autentication implementation
pick c14b97f Remove hardcoded credentials and update seed scripts
pick 119fb95 Remove hardcoded credentials and update seed scripts 1.0
```

---

## Paso 3: Marcar el commit problemático para editarlo

En la **primera línea** (la de `099b342`), cambia `pick` por `edit`:

```
edit 099b342 Resolve merge conflict keeping feature/autentication implementation
pick c14b97f Remove hardcoded credentials and update seed scripts
pick 119fb95 Remove hardcoded credentials and update seed scripts 1.0
```

Guarda el archivo y cierra el editor (en nano: Ctrl+O, Enter, Ctrl+X; en vim: `:wq`).

---

## Paso 4: Corregir el archivo en ese commit

Git se detendrá en el commit `099b342`. Sustituye el archivo de test por la versión segura (la que tenías al final de la rama) y améndalo:

```bash
git checkout ORIG_HEAD -- src/test/java/com/example/nexus/modules/auth/service/PasswordPolicyServiceImplTest.java
git add src/test/java/com/example/nexus/modules/auth/service/PasswordPolicyServiceImplTest.java
git commit --amend --no-edit
git rebase --continue
```

Si el editor se abre al hacer `rebase --continue`, solo guarda y cierra (sin cambiar nada).

---

## Paso 5: Subir la rama (reescrita) al remoto

Cuando el rebase termine sin errores:

```bash
git push --force-with-lease origin feature/autentication
```

`--force-with-lease` evita pisar cambios que alguien más haya subido a la misma rama.

---

## Listo

El PR se actualizará solo. GitGuardian volverá a escanear y ya no debería detectar los 6 secretos, porque el commit que los contenía ahora tiene la versión del test sin contraseñas en claro.

Si en el **Paso 4** ves conflictos al hacer `git rebase --continue`, dime en qué archivo y te indico cómo resolverlos.
