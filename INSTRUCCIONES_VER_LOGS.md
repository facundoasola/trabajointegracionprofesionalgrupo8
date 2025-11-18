# 🔍 INSTRUCCIONES - Ver Logs del Crash

## ⚡ Pasos Rápidos

### 1. Recompilar el Proyecto
```
Build > Clean Project
Build > Rebuild Project
```

### 2. Abrir Logcat ANTES de ejecutar
```
View > Tool Windows > Logcat
```

O usa el atajo: **Alt+6** (Windows/Linux) o **Cmd+6** (Mac)

### 3. Configurar Filtros en Logcat

#### Filtro por Tag:
En el campo de búsqueda de Logcat, escribe:
```
tag:MainActivity
```

O para ver todo:
```
package:com.example.saferouteapp
```

#### Filtro por Nivel:
Selecciona en el dropdown: **Verbose** (para ver todos los logs)

### 4. Ejecutar la App
```
Run > Run 'app'
```

### 5. Hacer Login
- Ingresa tus credenciales
- Click en "Login"
- **OBSERVA LOGCAT INMEDIATAMENTE**

### 6. Buscar los Logs

Deberías ver logs como:
```
D/MainActivity: === INICIO onCreate ===
D/MainActivity: Usuario logueado: test@saferoute.com
D/MainActivity: 1. Configurando OSMDroid...
D/MainActivity: 2. Inflando layout...
D/MainActivity: 3. Inicializando mapa...
D/MainActivity: 4. Inicializando campos de texto...
D/MainActivity: 5. Inicializando botones...
D/MainActivity: 6. Agregando marcador de usuario...
D/MainActivity: 7. Configurando puntos seguros...
D/MainActivity: 8. Cargando crímenes desde backend...
D/MainActivity: 9. Inicializando filtros...
D/MainActivity: === onCreate COMPLETADO EXITOSAMENTE ===
```

### 7. Identificar Dónde Falla

Si la app crashea, verás:
```
D/MainActivity: === INICIO onCreate ===
D/MainActivity: Usuario logueado: test@saferoute.com
D/MainActivity: 1. Configurando OSMDroid...
D/MainActivity: 2. Inflando layout...
E/MainActivity: === ERROR CRÍTICO EN onCreate ===
E/MainActivity: Mensaje: [MENSAJE DEL ERROR]
E/MainActivity: Tipo: [TIPO DE EXCEPCIÓN]
```

**El último log antes del error te dice dónde falló.**

---

## 📋 Qué Hacer Según el Error

### Error después de "1. Configurando OSMDroid..."
**Problema**: Error al configurar OSMDroid
**Solución**: Verificar permisos en AndroidManifest.xml

### Error después de "2. Inflando layout..."
**Problema**: Error en activity_main.xml
**Solución**: El layout tiene algún problema

### Error después de "3. Inicializando mapa..."
**Problema**: MapView no existe en el layout o tiene ID incorrecto
**Solución**: Verificar que en activity_main.xml existe:
```xml
<org.osmdroid.views.MapView
    android:id="@+id/map"
    ... />
```

### Error después de "4. Inicializando campos de texto..."
**Problema**: EditText no encontrados
**Solución**: Verificar IDs `origin_text` y `destination_text`

### Error después de "5. Inicializando botones..."
**Problema**: Algún botón no existe en el layout
**Solución**: Revisar todos los IDs de botones

### Error después de "6. Agregando marcador..."
**Problema**: Error al agregar marcador al mapa
**Solución**: Problema con los drawables

### Error después de "7. Configurando puntos seguros..."
**Problema**: Error en setupSafePoints o addSafePointsToMap
**Solución**: Problema con los iconos de policía u hospital

### Error después de "8. Cargando crímenes..."
**Problema**: Error al llamar al backend
**Solución**: Problema de conectividad o backend caído

---

## 🔴 Errores Comunes a Buscar en Logcat

### 1. NullPointerException
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.example.saferouteapp, PID: XXXXX
    java.lang.NullPointerException: Attempt to invoke virtual method ... on a null object reference
        at com.example.saferouteapp.MainActivity.onCreate(MainActivity.java:XXX)
```

**Qué hacer**: Mira el número de línea (XXX) y ese componente está null.

### 2. InflateException
```
E/AndroidRuntime: android.view.InflateException: Binary XML file line #XX: Error inflating class
    Caused by: android.content.res.Resources$NotFoundException: Drawable ... not found
```

**Qué hacer**: Falta un archivo drawable o recurso.

### 3. ResourceNotFoundException
```
E/AndroidRuntime: android.content.res.Resources$NotFoundException: Resource ID #0x...
```

**Qué hacer**: Algún ID no existe en el layout.

---

## 📤 Cómo Compartir los Logs Conmigo

### Opción 1: Copiar desde Logcat
1. En Logcat, haz clic derecho en el área de logs
2. Select All (Ctrl+A)
3. Copy (Ctrl+C)
4. Pega en un archivo de texto

### Opción 2: Exportar Logcat
1. Botón derecho en Logcat
2. "Export to Text File..."
3. Guarda el archivo
4. Compártelo

### Opción 3: Screenshot
1. Captura de pantalla de Logcat mostrando el error
2. Asegúrate de que se vea el error completo

---

## 🎯 Lo Que Necesito Ver

Por favor comparte:

1. **Los logs de MainActivity** (todos los que empiezan con D/MainActivity)
2. **El error completo** (las líneas rojas que empiezan con E/)
3. **El stack trace** (las líneas que dicen "at com.example.saferouteapp...")

Ejemplo de lo que necesito:
```
D/MainActivity: === INICIO onCreate ===
D/MainActivity: Usuario logueado: test@saferoute.com
D/MainActivity: 1. Configurando OSMDroid...
D/MainActivity: 2. Inflando layout...
D/MainActivity: 3. Inicializando mapa...
E/MainActivity: === ERROR CRÍTICO EN onCreate ===
E/MainActivity: Mensaje: MapView is null
E/MainActivity: Tipo: java.lang.RuntimeException
W/System.err: java.lang.RuntimeException: ERROR: MapView no encontrado en el layout
W/System.err:     at com.example.saferouteapp.MainActivity.onCreate(MainActivity.java:205)
W/System.err:     at android.app.Activity.performCreate(Activity.java:...)
```

---

## 💡 Soluciones Rápidas Mientras Debugueamos

### Solución Temporal 1: Comentar Crímenes del Backend
Si falla después de "8. Cargando crímenes...", comenta esta línea:

En MainActivity.java línea ~335:
```java
// loadCrimesFromBackend();  // COMENTAR TEMPORALMENTE
setupCrimeAlerts();  // Usar datos hardcodeados
addCrimeAlertsToMap();
```

### Solución Temporal 2: Simplificar onCreate
Comenta todo lo no esencial:

```java
// setupSafePoints();  // COMENTAR
// addSafePointsToMap();  // COMENTAR
// loadCrimesFromBackend();  // COMENTAR
```

Si funciona así, agregamos las funciones una por una para ver cuál falla.

---

## ✅ Próximos Pasos

1. ✅ Recompilar con los nuevos logs
2. ✅ Abrir Logcat
3. ✅ Ejecutar y hacer login
4. ✅ Copiar TODOS los logs
5. ✅ Compartirme los logs

Con esa información podré identificar **exactamente** qué está fallando y arreglarlo.

---

**IMPORTANTE**: Los logs ahora son mucho más detallados. Cada paso del onCreate tiene su propio log, así que sabremos exactamente dónde falla.

