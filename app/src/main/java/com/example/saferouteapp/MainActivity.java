package com.example.saferouteapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private MapView map;
    private EditText originEditText, destinationEditText;
    private Button routeButton, vehicleRouteButton;
    private ImageButton backButton;
    private Marker originMarker, destinationMarker;
    private FloatingActionButton menuButton, zoomInButton, zoomOutButton,
            streetCrimeFilterButton, vehicleCrimeFilterButton, myLocationButton;
    private Button reportCrimeButton;

    private final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibHVjYXNhZzA1IiwiYSI6ImNtZ3poaTdxMDAwOGcyaXBxYWRvYzJkanIifQ.2tJ3eYfxB8W5NbeQTKNHwA";

    private LinearLayout routeInfoLayout, searchLayout;
    private LinearLayout safeRouteOption, fastRouteOption;
    private LinearLayout exportButtonsLayout;
    private TextView safeRouteInfo, fastRouteInfo;
    private ImageView safeRouteRadio, fastRouteRadio;
    private Button exportUberButton, exportPedidosYaButton;

    private final List<Polyline> routeOverlays = new ArrayList<>();
    private final String GRAPHHOPPER_API_KEY = "34f7e5c8-bf47-4cb5-99e4-24d04d61ef0f";
    
    // Variables para almacenar ambas rutas
    private List<RouteInfo> safeRoutes = new ArrayList<>();
    private List<RouteInfo> fastRoutes = new ArrayList<>();
    private boolean safeRouteSelected = true;
    
    // Variables para filtros de crimen
    private boolean showStreetCrime = true;
    private boolean showVehicleCrime = true;
    private boolean vehicleMode = false; // Modo vehículo para rutas específicas
    
    // Sistema de puntos (solo se actualiza después de verificaciones)
    private int lastKnownPoints = 0;

    private static class SafePoint {
        final String name;
        final GeoPoint location;
        final String type;

        SafePoint(String name, GeoPoint location, String type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }
    }

    private static class CrimeAlert {
        final long id; // ID del reporte
        final String title;
        final String description;
        final String address; // Dirección que será geocodificada
        final String timeAgo;
        final String crimeType; // Tipo de crimen (mantener para compatibilidad)
        final String category; // "Delitos contra las personas" o "Delitos contra la propiedad"
        final String subType; // Subtipo específico del crimen
        final int severity; // Gravedad: 1 (leve) a 4 (grave)
        final String reporter; // Email del usuario que reportó
        final int verification; // Cantidad de verificaciones
        GeoPoint location; // Se establecerá después de la geocodificación
        final String status;

        CrimeAlert(long id, String title, String description, String address, String timeAgo, String crimeType,
                   String category, String subType, int severity, String reporter, int verification, String status) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.address = address;
            this.timeAgo = timeAgo;
            this.crimeType = crimeType;
            this.category = category;
            this.subType = subType;
            this.severity = severity;
            this.reporter = reporter;
            this.verification = verification;
            this.location = null; // Se establecerá más tarde
            this.status = status;
        }
        
        // Método auxiliar para obtener el color según la gravedad
        int getSeverityColor() {
            switch (severity) {
                case 4: return Color.parseColor("#8B0000"); // Rojo oscuro - Muy grave
                case 3: return Color.parseColor("#FF0000"); // Rojo - Grave
                case 2: return Color.parseColor("#FFA500"); // Naranja - Moderado
                case 1: return Color.parseColor("#FFD700"); // Amarillo - Leve
                default: return Color.parseColor("#FFA500"); // Por defecto naranja
            }
        }
        
        // Método para obtener el texto de la gravedad
        String getSeverityText() {
            switch (severity) {
                case 4: return "Muy Grave";
                case 3: return "Grave";
                case 2: return "Moderado";
                case 1: return "Leve";
                default: return "Desconocido";
            }
        }
    }

    private final List<SafePoint> safePoints = new ArrayList<>();
    private final List<Marker> safePointMarkers = new ArrayList<>();
    private final List<CrimeAlert> crimeAlerts = new ArrayList<>();
    private final List<Marker> crimeAlertMarkers = new ArrayList<>();
    private final List<Polygon> dangerZones = new ArrayList<>();
    private final List<Polygon> dangerZoneOverlays = new ArrayList<>();
    private boolean showDangerZones = false;
    
    // Ubicación actual del usuario (hardcodeada)
    private final GeoPoint currentUserLocation = new GeoPoint(-34.595183687496146, -58.3811805650211); // Av. Santa Fe 995, Buenos Aires
    private Marker userLocationMarker;


    private static class RouteInfo {
        final List<GeoPoint> points;
        final long timeInMillis;
        final double distanceInMeters;

        RouteInfo(List<GeoPoint> points, long timeInMillis, double distanceInMeters) {
            this.points = points;
            this.timeInMillis = timeInMillis;
            this.distanceInMeters = distanceInMeters;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("MainActivity", "=== INICIO onCreate ===");

        // Verificar que haya usuario logueado
        if (UserSession.getCurrentUser() == null) {
            android.util.Log.e("MainActivity", "ERROR: No hay usuario en sesión");
            Toast.makeText(this, "Por favor, inicia sesión", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        android.util.Log.d("MainActivity", "Usuario logueado: " + UserSession.getCurrentUserMail());

        try {
            android.util.Log.d("MainActivity", "1. Configurando OSMDroid...");
            Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("OSM", MODE_PRIVATE));
            Configuration.getInstance().setUserAgentValue(getPackageName());

            android.util.Log.d("MainActivity", "2. Inflando layout...");
            setContentView(R.layout.activity_main);

            android.util.Log.d("MainActivity", "3. Inicializando mapa...");
            map = findViewById(R.id.map);
            if (map == null) {
                throw new RuntimeException("ERROR: MapView no encontrado en el layout");
            }

        final XYTileSource mapboxTileSource = new XYTileSource("Mapbox", 0, 22, 256, ".png",
                new String[] { "https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/" }) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/"
                        + MapTileIndex.getY(pMapTileIndex) + "?access_token=" + MAPBOX_ACCESS_TOKEN;
            }
        };
        map.setTileSource(mapboxTileSource);

        map.setMultiTouchControls(true);
        
        // Configurar listener para tap en el mapa
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                setDestinationFromMapTap(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                // Mantener presionado: Mostrar diálogo para autocompletar origen o destino
                showLocationSelectionDialog(p);
                return true;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(0, mapEventsOverlay); // Agregar como primer overlay

        IMapController mapController = map.getController();
        mapController.setZoom(16.0); // Zoom para ver bien la ubicación del usuario
        // Centro inicial en la ubicación del usuario
        mapController.setCenter(currentUserLocation);

        android.util.Log.d("MainActivity", "4. Inicializando campos de texto...");
        originEditText = findViewById(R.id.origin_text);
        destinationEditText = findViewById(R.id.destination_text);
        
        if (originEditText == null || destinationEditText == null) {
            throw new RuntimeException("ERROR: EditText no encontrados");
        }

        // Configurar hint para mostrar la ubicación actual
        originEditText.setHint("📍 Ubicación actual (Av. Santa Fe 995)");
        
        android.util.Log.d("MainActivity", "5. Inicializando botones...");
        routeButton = findViewById(R.id.route_button);
        vehicleRouteButton = findViewById(R.id.vehicle_route_button);
        routeInfoLayout = findViewById(R.id.route_info_layout);
        searchLayout = findViewById(R.id.search_layout);
        safeRouteOption = findViewById(R.id.safe_route_option);
        fastRouteOption = findViewById(R.id.fast_route_option);
        safeRouteInfo = findViewById(R.id.safe_route_info);
        fastRouteInfo = findViewById(R.id.fast_route_info);
        safeRouteRadio = findViewById(R.id.safe_route_radio);
        fastRouteRadio = findViewById(R.id.fast_route_radio);
        menuButton = findViewById(R.id.menu_button);
        reportCrimeButton = findViewById(R.id.report_crime_button);
        backButton = findViewById(R.id.back_button);
        zoomInButton = findViewById(R.id.zoom_in_button);
        zoomOutButton = findViewById(R.id.zoom_out_button);
        myLocationButton = findViewById(R.id.my_location_button);
        streetCrimeFilterButton = findViewById(R.id.street_crime_filter_button);
        vehicleCrimeFilterButton = findViewById(R.id.vehicle_crime_filter_button);
        
        // Inicializar layout y botones de exportar
        exportButtonsLayout = findViewById(R.id.export_buttons_layout);
        exportUberButton = findViewById(R.id.export_uber_button);
        exportPedidosYaButton = findViewById(R.id.export_pedidosya_button);


        routeButton.setOnClickListener(v -> {
            String originAddress = originEditText.getText().toString().trim();
            String destinationAddress = destinationEditText.getText().toString().trim();

            if (destinationAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa un destino", Toast.LENGTH_SHORT).show();
                return;
            }

            vehicleMode = false;
            hideKeyboard();

            // Si el origen está vacío, usar directamente la ubicación actual (GeoPoint)
            if (originAddress.isEmpty()) {
                calculateBothRoutesFromCurrentLocation(destinationAddress);
            } else {
                calculateBothRoutes(originAddress, destinationAddress);
            }
        });
        
        vehicleRouteButton.setOnClickListener(v -> {
            String originAddress = originEditText.getText().toString().trim();
            String destinationAddress = destinationEditText.getText().toString().trim();

            if (destinationAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa un destino", Toast.LENGTH_SHORT).show();
                return;
            }

            vehicleMode = true;
            hideKeyboard();

            // Si el origen está vacío, usar directamente la ubicación actual (GeoPoint)
            if (originAddress.isEmpty()) {
                calculateBothRoutesFromCurrentLocation(destinationAddress);
            } else {
                calculateBothRoutes(originAddress, destinationAddress);
            }
        });

        // Configurar listeners para las opciones de ruta
        safeRouteOption.setOnClickListener(v -> selectRouteType(true));
        fastRouteOption.setOnClickListener(v -> selectRouteType(false));
        
        // Configurar listeners para botones de exportar
        exportUberButton.setOnClickListener(v -> exportToUber());
        exportPedidosYaButton.setOnClickListener(v -> exportToPedidosYa());

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            startActivity(intent);
        });
        reportCrimeButton.setOnClickListener(v -> showReportCrimeDialog());

        backButton.setOnClickListener(v -> clearRoute());

        zoomInButton.setOnClickListener(v -> map.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> map.getController().zoomOut());
        
        // Botón para volver a la ubicación actual
        myLocationButton.setOnClickListener(v -> {
            map.getController().animateTo(currentUserLocation);
            map.getController().setZoom(16.0);
            Toast.makeText(this, "📍 Ubicación actual: Av. Santa Fe 995", Toast.LENGTH_SHORT).show();
        });

        streetCrimeFilterButton.setOnClickListener(v -> toggleStreetCrimeFilter());
        vehicleCrimeFilterButton.setOnClickListener(v -> toggleVehicleCrimeFilter());

        android.util.Log.d("MainActivity", "6. Agregando marcador de usuario...");
        // Agregar marcador de ubicación actual del usuario
        addUserLocationMarker();
        
        android.util.Log.d("MainActivity", "7. Configurando puntos seguros...");
        setupSafePoints();

        android.util.Log.d("MainActivity", "7b. Agregando puntos seguros al mapa...");
        addSafePointsToMap();

        android.util.Log.d("MainActivity", "8. Programando carga de crímenes...");
        // Cargar crímenes desde el backend CON DELAY para evitar problemas de timing
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.util.Log.d("MainActivity", "8b. Ejecutando carga de crímenes...");
            loadCrimesFromBackend();
        }, 1000); // Esperar 1 segundo después de que el mapa esté listo

        android.util.Log.d("MainActivity", "9. Inicializando filtros...");
        // Inicializar filtros y botones
        showStreetCrime = true;
        showVehicleCrime = true;
        updateCrimeFilterButtons();
        
        // Marcar que las zonas de peligro deben mostrarse
        showDangerZones = true;
        // Las zonas se crearán después de geocodificar las alertas en addCrimeAlertsToMap()

        // Configurar manejo moderno del botón atrás
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si hay una ruta activa (panel de información visible), limpiar la ruta
                if (routeInfoLayout.getVisibility() == View.VISIBLE) {
                    clearRoute();
                } else {
                    // Si no hay ruta activa, comportamiento normal (cerrar app)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        android.util.Log.d("MainActivity", "=== onCreate COMPLETADO EXITOSAMENTE ===");

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "=== ERROR CRÍTICO EN onCreate ===");
            android.util.Log.e("MainActivity", "Mensaje: " + e.getMessage());
            android.util.Log.e("MainActivity", "Tipo: " + e.getClass().getName());
            e.printStackTrace();

            String errorMsg = "Error al inicializar: " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

            // Volver al login si hay error crítico
            try {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            } catch (Exception e2) {
                android.util.Log.e("MainActivity", "Error al volver a login: " + e2.getMessage());
            }
            finish();
        }
    }

    private void addUserLocationMarker() {
        try {
            // Crear marcador para la ubicación actual del usuario
            userLocationMarker = new Marker(map);
            userLocationMarker.setPosition(currentUserLocation);
            userLocationMarker.setTitle("Tu ubicación");
            userLocationMarker.setSnippet("Av. Santa Fe 995, Buenos Aires");
            userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            // Usar el icono personalizado de ubicación
            Drawable userLocationIcon = ContextCompat.getDrawable(this, R.drawable.ic_my_location);
            if (userLocationIcon != null) {
                userLocationMarker.setIcon(userLocationIcon);
            }

            // Hacer que el marcador sea más visible
            userLocationMarker.setAlpha(1.0f);

            // Agregar el marcador al mapa
            map.getOverlays().add(userLocationMarker);

            // Animar el marcador (efecto de pulso)
            startLocationMarkerAnimation();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error agregando marcador de usuario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startLocationMarkerAnimation() {
        // Crear un efecto de pulso simple usando un Handler (luz que prende y apaga)
        final android.os.Handler handler = new android.os.Handler();
        final Runnable pulseRunnable = new Runnable() {
            boolean growing = true;
            float alpha = 1.0f;

            @Override
            public void run() {
                if (userLocationMarker != null) {
                    if (growing) {
                        alpha += 0.05f;
                        if (alpha >= 1.0f) {
                            alpha = 1.0f;
                            growing = false;
                        }
                    } else {
                        alpha -= 0.05f;
                        if (alpha <= 0.6f) {
                            alpha = 0.6f;
                            growing = true;
                        }
                    }
                    userLocationMarker.setAlpha(alpha);
                    map.invalidate();
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(pulseRunnable);
    }
    
    private void startCrimeAlertAnimation(Marker marker) {
        // Crear animación de transparencia fluida para alertas de crimen
        final android.os.Handler handler = new android.os.Handler();
        final Runnable fadeRunnable = new Runnable() {
            float alpha = 1.0f;
            boolean fading = false;
            
            @Override
            public void run() {
                if (marker != null && map.getOverlays().contains(marker)) {
                    // Animación de transparencia suave (100% a 50%)
                    if (!fading) {
                        // Desvanecer de 1.0 a 0.5
                        alpha -= 0.01f;
                        if (alpha <= 0.5f) {
                            alpha = 0.5f;
                            fading = true;
                        }
                    } else {
                        // Intensificar de 0.5 a 1.0
                        alpha += 0.01f;
                        if (alpha >= 1.0f) {
                            alpha = 1.0f;
                            fading = false;
                        }
                    }

                    // Aplicar la transparencia al marcador
                    marker.setAlpha(alpha);
                    map.invalidate();

                    // Continuar la animación (más lento para efecto fluido)
                    handler.postDelayed(this, 30); // ~33fps para efecto más suave
                }
            }
        };
        handler.post(fadeRunnable);
    }

    private void setupSafePoints() {
        // Hospitales Públicos de CABA
        safePoints.add(new SafePoint("Hospital Durand", new GeoPoint(-34.6095, -58.4411), "hospital"));
        safePoints.add(new SafePoint("Hospital Fernández", new GeoPoint(-34.5828, -58.4203), "hospital"));
        safePoints.add(new SafePoint("Hospital Rivadavia", new GeoPoint(-34.5900, -58.4042), "hospital"));
        safePoints.add(new SafePoint("Hospital de Clínicas", new GeoPoint(-34.6011, -58.4052), "hospital"));
        safePoints.add(new SafePoint("Hospital Santojanni", new GeoPoint(-34.6508, -58.5028), "hospital"));
        safePoints.add(new SafePoint("Hospital Piñero", new GeoPoint(-34.6397, -58.4503), "hospital"));
        safePoints.add(new SafePoint("Hospital Pirovano", new GeoPoint(-34.5578, -58.4844), "hospital"));
        safePoints.add(new SafePoint("Hospital Zubizarreta", new GeoPoint(-34.6063, -58.5144), "hospital"));
        safePoints.add(new SafePoint("Hospital Penna", new GeoPoint(-34.6461, -58.4069), "hospital"));
        safePoints.add(new SafePoint("Hospital Elizalde", new GeoPoint(-34.6225, -58.3803), "hospital"));
        safePoints.add(new SafePoint("Hospital Garrahan", new GeoPoint(-34.6289, -58.3908), "hospital"));

        // Comisarías Vecinales de CABA (una por comuna como ejemplo)
        safePoints.add(new SafePoint("Comisaría Vecinal 1A", new GeoPoint(-34.6111, -58.3741), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 2B", new GeoPoint(-34.5941, -58.4098), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 3A", new GeoPoint(-34.6073, -58.4124), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 4D", new GeoPoint(-34.6391, -58.3712), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 5A", new GeoPoint(-34.6118, -58.4215), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 6B", new GeoPoint(-34.6186, -58.4533), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 7A", new GeoPoint(-34.6341, -58.4608), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 8A", new GeoPoint(-34.6738, -58.4674), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 9A", new GeoPoint(-34.6645, -58.5147), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 10A", new GeoPoint(-34.6288, -58.5042), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 11A", new GeoPoint(-34.6173, -58.4841), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 12A", new GeoPoint(-34.5614, -58.4981), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 13A", new GeoPoint(-34.5623, -58.4557), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 14A", new GeoPoint(-34.5775, -58.4312), "police"));
        safePoints.add(new SafePoint("Comisaría Vecinal 15A", new GeoPoint(-34.5901, -58.4439), "police"));
    }

    private void addSafePointsToMap() {
        try {
            Drawable policeIcon = ContextCompat.getDrawable(this, R.drawable.ic_police_station);
            Drawable hospitalIcon = ContextCompat.getDrawable(this, R.drawable.ic_hospital);

            for (SafePoint point : safePoints) {
                try {
                    Marker marker = new Marker(map);
                    marker.setPosition(point.location);
                    marker.setTitle(point.name);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                    if ("police".equals(point.type)) {
                        if (policeIcon != null) {
                            marker.setIcon(policeIcon);
                        }
                    } else {
                        if (hospitalIcon != null) {
                            marker.setIcon(hospitalIcon);
                        }
                    }
                    map.getOverlays().add(marker);
                    safePointMarkers.add(marker);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error agregando punto seguro: " + point.name);
                }
            }
            map.invalidate();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error general en addSafePointsToMap: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearRoute() {
        routeInfoLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
        originEditText.setText("");
        destinationEditText.setText("");
        map.getOverlays().removeAll(routeOverlays);
        routeOverlays.clear();
        
        // Limpiar las rutas almacenadas
        safeRoutes.clear();
        fastRoutes.clear();
        safeRouteSelected = true;
        
        if (originMarker != null) {
            map.getOverlays().remove(originMarker);
            originMarker = null;
        }
        if (destinationMarker != null) {
            map.getOverlays().remove(destinationMarker);
            destinationMarker = null;
        }
        
        // Limpiar marcadores de alertas para recrearlos
        clearCrimeAlertMarkersFromMap();
        
        // Volver a agregar los marcadores de puntos seguros y alertas de crimen
        addSafePointsToMap();
        addCrimeAlertsToMap(); // Esto volverá a geocodificar y agregar las alertas
        
        map.invalidate();
    }

    /**
     * Limpia todos los marcadores de crímenes del mapa
     * CRÍTICO: Previene el bug donde filtros cambian la apariencia de marcadores no confirmados
     */
    private void clearCrimeAlertMarkersFromMap() {
        System.out.println("🧹 Limpiando " + crimeAlertMarkers.size() + " marcadores de crímenes existentes");
        map.getOverlays().removeAll(crimeAlertMarkers);
        crimeAlertMarkers.clear();
    }

    private void showLocationSelectionDialog(GeoPoint point) {
        // Crear marcador temporal en el punto seleccionado
        final Marker tempMarker = new Marker(map);
        tempMarker.setPosition(point);
        tempMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        tempMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_destination_marker));
        tempMarker.setTitle("Ubicación seleccionada");
        map.getOverlays().add(tempMarker);
        map.invalidate();

        // Crear array de opciones para selección simple
        String[] options = {
            "📤 Establecer como Origen",
            "📥 Establecer como Destino",
            "🚨 Reportar Crimen Aquí"
        };

        final int[] selectedOption = {0}; // Por defecto la primera opción

        // Crear diálogo de selección
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📍 Ubicación seleccionada");

        // Usar SingleChoiceItems para mostrar las opciones con radio buttons
        builder.setSingleChoiceItems(options, 0, (dialog, which) -> {
            selectedOption[0] = which;
        });

        // Botón OK para confirmar la selección
        builder.setPositiveButton("OK", (dialog, which) -> {
            switch (selectedOption[0]) {
                case 0: // Establecer como origen
                    new Thread(() -> {
                        String address = getAddressFromGeoPoint(point);
                        runOnUiThread(() -> {
                            if (address != null) {
                                originEditText.setText(address);
                                Toast.makeText(MainActivity.this,
                                             "✅ Origen establecido",
                                             Toast.LENGTH_SHORT).show();
                            } else {
                                String coords = String.format("%.6f, %.6f",
                                                            point.getLatitude(),
                                                            point.getLongitude());
                                originEditText.setText(coords);
                                Toast.makeText(MainActivity.this,
                                             "✅ Origen establecido (coordenadas)",
                                             Toast.LENGTH_SHORT).show();
                            }
                            map.getOverlays().remove(tempMarker);
                            map.invalidate();
                        });
                    }).start();
                    break;

                case 1: // Establecer como destino
                    new Thread(() -> {
                        String address = getAddressFromGeoPoint(point);
                        runOnUiThread(() -> {
                            if (address != null) {
                                destinationEditText.setText(address);
                                Toast.makeText(MainActivity.this,
                                             "✅ Destino establecido",
                                             Toast.LENGTH_SHORT).show();
                            } else {
                                String coords = String.format("%.6f, %.6f",
                                                            point.getLatitude(),
                                                            point.getLongitude());
                                destinationEditText.setText(coords);
                                Toast.makeText(MainActivity.this,
                                             "✅ Destino establecido (coordenadas)",
                                             Toast.LENGTH_SHORT).show();
                            }
                            map.getOverlays().remove(tempMarker);
                            map.invalidate();
                        });
                    }).start();
                    break;

                case 2: // Reportar crimen aquí
                    map.getOverlays().remove(tempMarker);
                    map.invalidate();
                    // Obtener dirección y abrir diálogo de reporte con ubicación prellenada
                    new Thread(() -> {
                        String address = getAddressFromGeoPoint(point);
                        runOnUiThread(() -> {
                            showReportCrimeDialogWithLocation(point, address);
                        });
                    }).start();
                    break;
            }
        });

        // Botón Cancelar
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            map.getOverlays().remove(tempMarker);
            map.invalidate();
        });

        // Cuando se cancela el diálogo, remover el marcador temporal
        builder.setOnCancelListener(dialog -> {
            map.getOverlays().remove(tempMarker);
            map.invalidate();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getAddressFromGeoPoint(GeoPoint point) {
        try {
            // Usar Nominatim API para geocodificación inversa
            String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" +
                        point.getLatitude() + "&lon=" + point.getLongitude() +
                        "&zoom=18&addressdetails=1";

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "SafeRouteApp");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parsear JSON
                JSONObject jsonResponse = new JSONObject(response.toString());

                // Intentar obtener la dirección en formato display_name
                if (jsonResponse.has("display_name")) {
                    return jsonResponse.getString("display_name");
                }

                // Si no hay display_name, construir desde address
                if (jsonResponse.has("address")) {
                    JSONObject address = jsonResponse.getJSONObject("address");
                    StringBuilder addressStr = new StringBuilder();

                    // Construir dirección con los componentes disponibles
                    if (address.has("road")) {
                        addressStr.append(address.getString("road"));
                    }
                    if (address.has("house_number")) {
                        if (addressStr.length() > 0) addressStr.append(" ");
                        addressStr.append(address.getString("house_number"));
                    }
                    if (address.has("suburb") || address.has("neighbourhood")) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(address.optString("suburb", address.optString("neighbourhood")));
                    }
                    if (address.has("city")) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(address.getString("city"));
                    }
                    if (address.has("country")) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(address.getString("country"));
                    }

                    if (addressStr.length() > 0) {
                        return addressStr.toString();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error en geocodificación inversa: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void calculateBothRoutes(String originAddress, String destinationAddress) {
        new Thread(() -> {
            try {
                GeoPoint originPoint = getGeoPointFromAddress(originAddress);
                GeoPoint destinationPoint = getGeoPointFromAddress(destinationAddress);
                if (originPoint == null || destinationPoint == null) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo encontrar una de las direcciones", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Calcular ruta rápida (directa)
                fastRoutes = getRoutes(originPoint, destinationPoint);
                if (fastRoutes == null || fastRoutes.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo calcular ninguna ruta", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Calcular ruta segura usando waypoints que eviten zonas peligrosas
                safeRoutes = getSafeRoute(originPoint, destinationPoint);
                if (safeRoutes.isEmpty()) {
                    // Fallback: filtrar las rutas normales por seguridad
                    List<RouteInfo> filteredSafeRoutes = filterSafeRoutes(fastRoutes);
                    safeRoutes = filteredSafeRoutes.isEmpty() ? fastRoutes : filteredSafeRoutes;
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Usando ruta con menor riesgo disponible.", Toast.LENGTH_LONG).show());
                }

                // Debug: Verificar que las rutas son diferentes
                if (!fastRoutes.isEmpty() && !safeRoutes.isEmpty()) {
                    RouteInfo fastRoute = fastRoutes.get(0);
                    RouteInfo safeRoute = safeRoutes.get(0);

                    System.out.println("=== DEBUG RUTAS ===");
                    System.out.println("Ruta rápida - Tiempo: " + fastRoute.timeInMillis/1000 + "s, Distancia: " + fastRoute.distanceInMeters + "m");
                    System.out.println("Ruta segura - Tiempo: " + safeRoute.timeInMillis/1000 + "s, Distancia: " + safeRoute.distanceInMeters + "m");
                    System.out.println("¿Son la misma ruta? " + (fastRoute == safeRoute));
                    System.out.println("Riesgo ruta rápida: " + calculateRouteSafetyScore(fastRoute));
                    System.out.println("Riesgo ruta segura: " + calculateRouteSafetyScore(safeRoute));
                }

                runOnUiThread(() -> {
                    drawBothRoutes(originPoint, destinationPoint);
                    showRouteOptions();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al calcular las rutas", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void calculateBothRoutesFromCurrentLocation(String destinationAddress) {
        new Thread(() -> {
            try {
                android.util.Log.d("MainActivity", "🚀 Iniciando cálculo de rutas desde ubicación actual");
                android.util.Log.d("MainActivity", "📍 Ubicación actual: " + currentUserLocation.getLatitude() + ", " + currentUserLocation.getLongitude());
                android.util.Log.d("MainActivity", "🎯 Destino: " + destinationAddress);

                // Validar que currentUserLocation no sea null
                if (currentUserLocation == null) {
                    android.util.Log.e("MainActivity", "❌ ERROR: currentUserLocation es null");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: No se pudo obtener tu ubicación actual", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Usar directamente la ubicación actual sin geocodificar
                GeoPoint originPoint = currentUserLocation;

                // Solo geocodificar el destino
                android.util.Log.d("MainActivity", "🔍 Geocodificando destino: " + destinationAddress);
                GeoPoint destinationPoint = getGeoPointFromAddress(destinationAddress);
                if (destinationPoint == null) {
                    android.util.Log.e("MainActivity", "❌ ERROR: No se pudo geocodificar el destino");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo encontrar la dirección de destino", Toast.LENGTH_SHORT).show());
                    return;
                }
                android.util.Log.d("MainActivity", "✅ Destino geocodificado: " + destinationPoint.getLatitude() + ", " + destinationPoint.getLongitude());

                // Calcular ruta rápida (directa)
                android.util.Log.d("MainActivity", "⚡ Calculando ruta rápida...");
                fastRoutes = getRoutes(originPoint, destinationPoint);
                if (fastRoutes == null || fastRoutes.isEmpty()) {
                    android.util.Log.e("MainActivity", "❌ ERROR: No se pudo calcular ruta rápida");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo calcular ninguna ruta", Toast.LENGTH_SHORT).show());
                    return;
                }
                android.util.Log.d("MainActivity", "✅ Ruta rápida calculada: " + fastRoutes.size() + " alternativas");

                // Calcular ruta segura usando waypoints que eviten zonas peligrosas
                android.util.Log.d("MainActivity", "🛡️ Calculando ruta segura...");
                safeRoutes = getSafeRoute(originPoint, destinationPoint);
                if (safeRoutes.isEmpty()) {
                    android.util.Log.w("MainActivity", "⚠️ No se pudo calcular ruta segura, usando fallback");
                    // Fallback: filtrar las rutas normales por seguridad
                    List<RouteInfo> filteredSafeRoutes = filterSafeRoutes(fastRoutes);
                    safeRoutes = filteredSafeRoutes.isEmpty() ? fastRoutes : filteredSafeRoutes;
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Usando ruta con menor riesgo disponible.", Toast.LENGTH_LONG).show());
                } else {
                    android.util.Log.d("MainActivity", "✅ Ruta segura calculada");
                }

                // Debug: Verificar que las rutas son diferentes
                if (!fastRoutes.isEmpty() && !safeRoutes.isEmpty()) {
                    RouteInfo fastRoute = fastRoutes.get(0);
                    RouteInfo safeRoute = safeRoutes.get(0);

                    android.util.Log.d("MainActivity", "=== COMPARACIÓN DE RUTAS ===");
                    android.util.Log.d("MainActivity", "Origen: Ubicación actual (" + originPoint.getLatitude() + ", " + originPoint.getLongitude() + ")");
                    android.util.Log.d("MainActivity", "Ruta rápida - Tiempo: " + fastRoute.timeInMillis/1000 + "s, Distancia: " + fastRoute.distanceInMeters + "m");
                    android.util.Log.d("MainActivity", "Ruta segura - Tiempo: " + safeRoute.timeInMillis/1000 + "s, Distancia: " + safeRoute.distanceInMeters + "m");
                    android.util.Log.d("MainActivity", "Riesgo ruta rápida: " + calculateRouteSafetyScore(fastRoute));
                    android.util.Log.d("MainActivity", "Riesgo ruta segura: " + calculateRouteSafetyScore(safeRoute));
                }

                android.util.Log.d("MainActivity", "🎨 Dibujando rutas en el mapa...");
                runOnUiThread(() -> {
                    drawBothRoutes(originPoint, destinationPoint);
                    showRouteOptions();
                    Toast.makeText(MainActivity.this, "📍 Ruta desde tu ubicación actual", Toast.LENGTH_SHORT).show();
                    android.util.Log.d("MainActivity", "✅ Rutas dibujadas correctamente");
                });

            } catch (Exception e) {
                android.util.Log.e("MainActivity", "❌ ERROR CRÍTICO al calcular rutas desde ubicación actual", e);
                e.printStackTrace();
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al calcular las rutas: " + errorMsg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void selectRouteType(boolean selectSafe) {
        safeRouteSelected = selectSafe;
        
        // Actualizar UI
        if (selectSafe) {
            // Ruta segura seleccionada
            safeRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            fastRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            safeRouteRadio.setImageResource(android.R.drawable.radiobutton_on_background);
            fastRouteRadio.setImageResource(android.R.drawable.radiobutton_off_background);
        } else {
            // Ruta rápida seleccionada
            safeRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            fastRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            safeRouteRadio.setImageResource(android.R.drawable.radiobutton_off_background);
            fastRouteRadio.setImageResource(android.R.drawable.radiobutton_on_background);
        }

        // Redibujar rutas con la selección actualizada
        drawSelectedRoute();
    }

    private void drawBothRoutes(GeoPoint originPoint, GeoPoint destinationPoint) {
        // Limpiar rutas anteriores
        map.getOverlays().removeAll(routeOverlays);
        routeOverlays.clear();

        // Agregar marcadores de origen y destino
        addRouteMarkers(originPoint, destinationPoint);

        // Recopilar todos los puntos para el zoom
        List<GeoPoint> allPoints = new ArrayList<>();
        allPoints.add(originPoint);
        allPoints.add(destinationPoint);

        // Dibujar ruta rápida con color según el modo
        if (!fastRoutes.isEmpty()) {
            RouteInfo fastRoute = fastRoutes.get(0);
            int fastColor = vehicleMode ? Color.parseColor("#FF6600") : Color.RED; // Naranja para vehículo, rojo para peatón
            drawSingleRoute(fastRoute.points, fastColor, 6.0f);
            allPoints.addAll(fastRoute.points);
        }

        // Dibujar ruta segura con color según el modo
        if (!safeRoutes.isEmpty()) {
            RouteInfo safeRoute = safeRoutes.get(0);
            int safeColor = vehicleMode ? Color.parseColor("#00AA00") : Color.BLUE; // Verde para vehículo, azul para peatón
            drawSingleRoute(safeRoute.points, safeColor, 8.0f);
            allPoints.addAll(safeRoute.points);
        }

        // Hacer zoom para mostrar toda la ruta con padding
        if (!allPoints.isEmpty()) {
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(allPoints);
            map.zoomToBoundingBox(boundingBox, true, 150);
        }

        map.invalidate();
    }

    private void drawSelectedRoute() {
        // Limpiar rutas anteriores
        map.getOverlays().removeAll(routeOverlays);
        routeOverlays.clear();

        List<RouteInfo> selectedRoutes = safeRouteSelected ? safeRoutes : fastRoutes;
        int routeColor;
        
        if (safeRouteSelected) {
            routeColor = vehicleMode ? Color.parseColor("#00AA00") : Color.BLUE; // Verde para vehículo seguro, azul para peatón seguro
        } else {
            routeColor = vehicleMode ? Color.parseColor("#FF6600") : Color.RED; // Naranja para vehículo rápido, rojo para peatón rápido
        }

        if (!selectedRoutes.isEmpty()) {
            RouteInfo selectedRoute = selectedRoutes.get(0);
            drawSingleRoute(selectedRoute.points, routeColor, 8.0f);
        }

        map.invalidate();
    }

    private void drawSingleRoute(List<GeoPoint> routePoints, int color, float width) {
        Polyline routeLine = new Polyline();
        routeLine.setPoints(routePoints);
        routeLine.setColor(color);
        routeLine.setWidth(width);
        routeOverlays.add(routeLine);
        map.getOverlays().add(routeLine);
    }

    private void showRouteOptions() {
        // Actualizar información de las rutas
        if (!safeRoutes.isEmpty()) {
            RouteInfo safeRoute = safeRoutes.get(0);
            String safeLabel = vehicleMode ? "Ruta Segura (Vehículo)" : "Ruta Segura (Peatón)";
            String safeInfo = String.format(Locale.getDefault(), "%s - %s (%s)",
                    safeLabel,
                    formatDuration(safeRoute.timeInMillis),
                    formatDistance(safeRoute.distanceInMeters));
            safeRouteInfo.setText(safeInfo);
        }

        if (!fastRoutes.isEmpty()) {
            RouteInfo fastRoute = fastRoutes.get(0);
            String fastLabel = vehicleMode ? "Ruta Directa (Vehículo)" : "Ruta Directa (Peatón)";
            String fastInfo = String.format(Locale.getDefault(), "%s - %s (%s)",
                    fastLabel,
                    formatDuration(fastRoute.timeInMillis),
                    formatDistance(fastRoute.distanceInMeters));
            fastRouteInfo.setText(fastInfo);
        }

        // Mostrar panel de opciones
        searchLayout.setVisibility(View.GONE);
        routeInfoLayout.setVisibility(View.VISIBLE);
        
        // Configurar visibilidad de botones de exportar según el modo
        if (vehicleMode) {
            // Modo vehículo: mostrar ambos botones
            exportUberButton.setVisibility(View.VISIBLE);
            exportPedidosYaButton.setVisibility(View.VISIBLE);
            
            // Ajustar layout para dos botones
            LinearLayout.LayoutParams uberParams = (LinearLayout.LayoutParams) exportUberButton.getLayoutParams();
            uberParams.weight = 1;
            exportUberButton.setLayoutParams(uberParams);
            
            LinearLayout.LayoutParams pedidosParams = (LinearLayout.LayoutParams) exportPedidosYaButton.getLayoutParams();
            pedidosParams.weight = 1;
            exportPedidosYaButton.setLayoutParams(pedidosParams);
        } else {
            // Modo peatón: solo mostrar Pedidos Ya
            exportUberButton.setVisibility(View.GONE);
            exportPedidosYaButton.setVisibility(View.VISIBLE);
            
            // Ajustar layout para un solo botón centrado
            LinearLayout.LayoutParams pedidosParams = (LinearLayout.LayoutParams) exportPedidosYaButton.getLayoutParams();
            pedidosParams.weight = 0;
            pedidosParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            pedidosParams.setMargins(0, 0, 0, 0);
            exportPedidosYaButton.setLayoutParams(pedidosParams);
        }

        // Seleccionar ruta segura por defecto
        selectRouteType(true);
        
        // Mostrar mensaje específico para modo vehículo
        if (vehicleMode) {
            Toast.makeText(this, "Rutas optimizadas para vehículo - Evitando robos de vehículos", Toast.LENGTH_LONG).show();
        }
    }

    private void addRouteMarkers(GeoPoint originPoint, GeoPoint destinationPoint) {
        // Limpiar marcadores anteriores
        if (originMarker != null) {
            map.getOverlays().remove(originMarker);
            originMarker = null;
        }
        if (destinationMarker != null) {
            map.getOverlays().remove(destinationMarker);
            destinationMarker = null;
        }

        // Crear marcador de origen
        originMarker = new Marker(map);
        originMarker.setPosition(originPoint);
        originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_origin_marker));
        originMarker.setTitle("Inicio");
        map.getOverlays().add(originMarker);

        // Crear marcador de destino
        destinationMarker = new Marker(map);
        destinationMarker.setPosition(destinationPoint);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_destination_marker));
        destinationMarker.setTitle("Destino");
        map.getOverlays().add(destinationMarker);
    }

    private void findAndDrawRoute(String originAddress, String destinationAddress, boolean isSafeRoute) {
        String routeType = isSafeRoute ? "ruta segura" : "rutas";
        Toast.makeText(this, "Calculando " + routeType + "...", Toast.LENGTH_SHORT).show();
        routeInfoLayout.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                GeoPoint originPoint = getGeoPointFromAddress(originAddress);
                GeoPoint destinationPoint = getGeoPointFromAddress(destinationAddress);
                if (originPoint == null || destinationPoint == null) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo encontrar una de las direcciones", Toast.LENGTH_SHORT).show());
                    return;
                }
                List<RouteInfo> routes = getRoutes(originPoint, destinationPoint);
                if (routes == null || routes.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo calcular ninguna ruta", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                final List<RouteInfo> finalRoutes;
                if (isSafeRoute) {
                    // Filtrar y ordenar rutas por seguridad
                    List<RouteInfo> safeRoutes = filterSafeRoutes(routes);
                    if (safeRoutes.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se encontraron rutas seguras. Mostrando ruta más segura disponible.", Toast.LENGTH_LONG).show());
                        // Si no hay rutas seguras, usar la ruta original pero marcarla como la más segura disponible
                        finalRoutes = routes;
                    } else {
                        finalRoutes = safeRoutes;
                    }
                } else {
                    finalRoutes = routes;
                }
                
                runOnUiThread(() -> drawRoutes(finalRoutes, originPoint, destinationPoint, isSafeRoute));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al calcular la ruta", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private GeoPoint getGeoPointFromAddress(String address) throws IOException, JSONException {
        String encodedAddress = URLEncoder.encode(address, "UTF-8");
        URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=1&countrycodes=ar");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", getPackageName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        JSONArray jsonArray = new JSONArray(result.toString());
        if (jsonArray.length() > 0) {
            JSONObject obj = jsonArray.getJSONObject(0);
            return new GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"));
        }
        return null;
    }

    private List<RouteInfo> getRoutes(GeoPoint start, GeoPoint end) throws IOException, JSONException {
        String vehicle = vehicleMode ? "car" : "foot";
        String urlString = "https://graphhopper.com/api/1/route?point=" + start.getLatitude() + "," + start.getLongitude() +
                "&point=" + end.getLatitude() + "," + end.getLongitude() +
                "&vehicle=" + vehicle + "&key=" + GRAPHHOPPER_API_KEY + 
                "&alternative_route.max_paths=3&alternative_route.max_weight_factor=1.4&alternative_route.max_share_factor=0.6&points_encoded=true";
        URL url = new URL(urlString);
        System.out.println("GraphHopper URL (" + (vehicleMode ? "vehicle" : "walking") + "): " + urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        JSONObject jsonObject = new JSONObject(result.toString());
        JSONArray paths = jsonObject.getJSONArray("paths");
        List<RouteInfo> routes = new ArrayList<>();
        
        System.out.println("=== GraphHopper Response ===");
        System.out.println("Número de rutas devueltas: " + paths.length());
        
        for (int i = 0; i < paths.length(); i++) {
            JSONObject path = paths.getJSONObject(i);
            String encodedPolyline = path.getString("points");
            List<GeoPoint> points = decodePolyline(encodedPolyline);
            long time = path.getLong("time");
            double distance = path.getDouble("distance");
            routes.add(new RouteInfo(points, time, distance));
            
            System.out.println("Ruta " + (i+1) + " - Tiempo: " + time/1000 + "s, Distancia: " + distance + "m, Puntos: " + points.size());
        }
        
        return routes;
    }

    private List<RouteInfo> getSafeRoute(GeoPoint start, GeoPoint end) throws IOException, JSONException {
        System.out.println("\n=== CALCULANDO RUTA SEGURA ===");
        System.out.println("📊 Reportes cargados en memoria: " + crimeAlerts.size());

        // Primero, verificar la ruta directa
        List<RouteInfo> directRoute = getRoutes(start, end);
        if (directRoute.isEmpty()) {
            System.out.println("❌ No se pudo obtener ruta directa");
            return directRoute;
        }
        
        System.out.println("🗺️ Ruta directa obtenida (" + directRoute.get(0).points.size() + " puntos)");

        // Analizar si la ruta directa pasa por zonas peligrosas
        List<CrimeAlert> dangerousPoints = findDangersInRoute(directRoute.get(0));
        
        if (dangerousPoints.isEmpty()) {
            System.out.println("✅ Ruta directa es segura, no hay peligros en el camino");
            return directRoute;
        }
        
        System.out.println("⚠️ Ruta directa pasa por " + dangerousPoints.size() + " zona(s) de peligro");
        for (CrimeAlert danger : dangerousPoints) {
            System.out.println("  - " + danger.subType + " (Nivel " + danger.severity + ") en " + danger.address);
        }
        
        // Generar waypoints para evitar las zonas peligrosas
        List<GeoPoint> avoidanceWaypoints = generateAvoidanceWaypoints(start, end, dangerousPoints);
        
        if (avoidanceWaypoints.isEmpty()) {
            System.out.println("❌ No se pueden generar waypoints de evasión, usando ruta directa");
            return directRoute;
        }
        
        System.out.println("🔄 Generando ruta alternativa con " + avoidanceWaypoints.size() + " waypoint(s) de evasión");

        // Construir ruta con waypoints
        List<RouteInfo> safeRoute = buildRouteWithWaypoints(start, end, avoidanceWaypoints);

        // VERIFICACIÓN FINAL: Confirmar que la ruta segura realmente evita los peligros
        if (!safeRoute.isEmpty()) {
            System.out.println("\n🔍 VERIFICACIÓN FINAL DE RUTA SEGURA:");
            List<CrimeAlert> remainingDangers = findDangersInRoute(safeRoute.get(0));

            if (remainingDangers.isEmpty()) {
                System.out.println("✅ ¡ÉXITO! Ruta segura verificada - NO pasa por zonas de peligro");
            } else {
                System.out.println("⚠️ ADVERTENCIA: Ruta segura aún pasa por " + remainingDangers.size() + " peligro(s)");
                for (CrimeAlert danger : remainingDangers) {
                    System.out.println("  - " + danger.subType + " (Nivel " + danger.severity + ")");
                }
                System.out.println("💡 Sugerencia: Aumentar desviaciones o agregar más waypoints");
            }
        }

        return safeRoute;
    }
    
    private List<CrimeAlert> findDangersInRoute(RouteInfo route) {
        List<CrimeAlert> dangers = new ArrayList<>();
        
        System.out.println("🔍 Analizando ruta con " + route.points.size() + " puntos");
        System.out.println("📊 Reportes totales cargados: " + crimeAlerts.size());

        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location == null) continue;
            
            // Solo considerar crímenes CONFIRMADOS
            if (!"CONFIRMADO".equals(crime.status)) {
                continue;
            }
            
            // Solo considerar crímenes relevantes al modo de transporte
            if (vehicleMode) {
                // Modo vehículo: solo crímenes de propiedad/vehículos
                if (!"Delitos contra la propiedad".equals(crime.category)) {
                    continue;
                }
            } else {
                // Modo transeúnte: solo crímenes contra personas
                if (!"Delitos contra las personas".equals(crime.category)) {
                    continue;
                }
            }
            
            // Radio de detección según gravedad y modo de transporte
            double dangerRadius;
            if (vehicleMode) {
                // Vehículos: radios más grandes (menos libertad de movimiento)
                switch (crime.severity) {
                    case 1: dangerRadius = 40; break;
                    case 2: dangerRadius = 80; break;
                    case 3: dangerRadius = 180; break;
                    case 4: dangerRadius = 250; break;
                    default: dangerRadius = 100; break;
                }
            } else {
                // Peatones: radios más pequeños (más libertad de movimiento)
                switch (crime.severity) {
                    case 1: dangerRadius = 25; break;
                    case 2: dangerRadius = 50; break;
                    case 3: dangerRadius = 100; break;
                    case 4: dangerRadius = 150; break;
                    default: dangerRadius = 75; break;
                }
            }
            
            // Verificar TODOS los puntos de la ruta (verificación exhaustiva)
            boolean foundDanger = false;
            for (GeoPoint point : route.points) {
                double distance = calculateDistance(point, crime.location);
                if (distance <= dangerRadius) {
                    if (!dangers.contains(crime)) {
                        dangers.add(crime);
                        System.out.println("⚠️ PELIGRO DETECTADO: " + crime.subType +
                                         " (Nivel " + crime.severity + ", Radio: " + dangerRadius + "m, Distancia: " +
                                         String.format("%.0f", distance) + "m)");
                        foundDanger = true;
                    }
                    break;
                }
            }
        }
        
        if (dangers.isEmpty()) {
            System.out.println("✅ Ruta verificada: NO pasa por zonas de peligro");
        } else {
            System.out.println("🚨 Ruta pasa por " + dangers.size() + " zona(s) de peligro - GENERANDO DESVÍOS");
        }

        return dangers;
    }
    
    private List<GeoPoint> generateAvoidanceWaypoints(GeoPoint start, GeoPoint end, List<CrimeAlert> dangers) {
        List<GeoPoint> waypoints = new ArrayList<>();
        
        System.out.println("\n🔧 GENERANDO WAYPOINTS DE EVASIÓN:");

        // Calcular vector de dirección de la ruta
        double dx = end.getLongitude() - start.getLongitude();
        double dy = end.getLatitude() - start.getLatitude();
        double routeLength = Math.sqrt(dx * dx + dy * dy);
        
        if (routeLength == 0) return waypoints;
        
        // Vector perpendicular normalizado (para desviar a los lados)
        double perpX = -dy / routeLength;
        double perpY = dx / routeLength;
        
        // Ordenar peligros por su posición a lo largo de la ruta
        List<CrimeAlert> sortedDangers = new ArrayList<>(dangers);
        sortedDangers.sort((d1, d2) -> {
            double t1 = ((d1.location.getLatitude() - start.getLatitude()) * dy +
                        (d1.location.getLongitude() - start.getLongitude()) * dx) /
                        (routeLength * routeLength);
            double t2 = ((d2.location.getLatitude() - start.getLatitude()) * dy +
                        (d2.location.getLongitude() - start.getLongitude()) * dx) /
                        (routeLength * routeLength);
            return Double.compare(t1, t2);
        });

        for (CrimeAlert danger : sortedDangers) {
            // Calcular el punto de la ruta más cercano al peligro
            double t = ((danger.location.getLatitude() - start.getLatitude()) * dy + 
                       (danger.location.getLongitude() - start.getLongitude()) * dx) / 
                       (routeLength * routeLength);
            
            // Limitar t entre 0 y 1 (dentro de la ruta)
            t = Math.max(0, Math.min(1, t));
            
            // Punto más cercano en la ruta al peligro
            double closestLat = start.getLatitude() + t * dy;
            double closestLon = start.getLongitude() + t * dx;
            GeoPoint closestPoint = new GeoPoint(closestLat, closestLon);
            
            // Distancia del punto más cercano al peligro
            double distanceToDanger = calculateDistance(closestPoint, danger.location);
            
            // Radio de peligro (mismo que en findDangersInRoute)
            double dangerRadius;
            switch (danger.severity) {
                case 1: dangerRadius = 60; break;
                case 2: dangerRadius = 120; break;
                case 3: dangerRadius = 270; break;
                case 4: dangerRadius = 375; break;
                default: dangerRadius = 150; break;
            }
            
            // DESVIACIÓN MASIVA: radio completo + 200m mínimo de seguridad
            double minSafeDistance = dangerRadius + 200; // Garantizar estar MUY lejos
            double deviationMeters = Math.max(minSafeDistance, dangerRadius * 1.8); // Al menos 1.8x el radio
            double deviationDistance = deviationMeters / 111000.0; // Convertir a grados

            System.out.println("  📍 Peligro: " + danger.subType + " (Nivel " + danger.severity + ")");
            System.out.println("     Radio: " + dangerRadius + "m, Desviación: " + deviationMeters + "m");

            // Generar múltiples candidatos de waypoint en diferentes direcciones
            List<GeoPoint> candidates = new ArrayList<>();

            // 1. Perpendicular derecha
            candidates.add(new GeoPoint(
                closestLat + perpY * deviationDistance,
                closestLon + perpX * deviationDistance
            ));

            // 2. Perpendicular izquierda
            candidates.add(new GeoPoint(
                closestLat - perpY * deviationDistance,
                closestLon - perpX * deviationDistance
            ));

            // 3. Diagonal derecha-adelante
            candidates.add(new GeoPoint(
                closestLat + (perpY * 0.7 + dy/routeLength * 0.3) * deviationDistance,
                closestLon + (perpX * 0.7 + dx/routeLength * 0.3) * deviationDistance
            ));

            // 4. Diagonal izquierda-adelante
            candidates.add(new GeoPoint(
                closestLat + (-perpY * 0.7 + dy/routeLength * 0.3) * deviationDistance,
                closestLon + (-perpX * 0.7 + dx/routeLength * 0.3) * deviationDistance
            ));

            // Evaluar candidatos y elegir el mejor
            GeoPoint bestWaypoint = null;
            double bestScore = -1;

            for (GeoPoint candidate : candidates) {
                // VERIFICACIÓN ESTRICTA: El waypoint debe estar FUERA de TODOS los círculos
                boolean isSafe = true;
                double minDistToAnyDanger = Double.MAX_VALUE;

                for (CrimeAlert checkDanger : crimeAlerts) {
                    if (checkDanger.location == null) continue;

                    double checkRadius;
                    switch (checkDanger.severity) {
                        case 1: checkRadius = 60; break;
                        case 2: checkRadius = 120; break;
                        case 3: checkRadius = 270; break;
                        case 4: checkRadius = 375; break;
                        default: checkRadius = 150; break;
                    }

                    double distToCheck = calculateDistance(candidate, checkDanger.location);
                    minDistToAnyDanger = Math.min(minDistToAnyDanger, distToCheck);

                    // Si está dentro de algún círculo (+50m buffer), NO es válido
                    if (distToCheck < checkRadius + 50) {
                        isSafe = false;
                        break;
                    }
                }

                // Solo considerar waypoints seguros
                if (isSafe) {
                    // Score: preferir los que están más lejos de todos los peligros
                    double score = minDistToAnyDanger;
                    if (score > bestScore) {
                        bestScore = score;
                        bestWaypoint = candidate;
                    }
                }
            }

            // Agregar el mejor waypoint encontrado
            if (bestWaypoint != null) {
                waypoints.add(bestWaypoint);
                System.out.println("     ✅ Waypoint agregado (distancia segura mín: " +
                                 String.format("%.0f", bestScore) + "m)");
            } else {
                System.out.println("     ⚠️ No se encontró waypoint seguro, intentando desviación mayor");

                // Último recurso: desviación EXTREMA
                double extremeDeviation = deviationMeters * 2.5 / 111000.0;
                GeoPoint extremeWaypoint = new GeoPoint(
                    closestLat + perpY * extremeDeviation,
                    closestLon + perpX * extremeDeviation
                );
                waypoints.add(extremeWaypoint);
                System.out.println("     🆘 Waypoint extremo agregado (desviación: " +
                                 String.format("%.0f", deviationMeters * 2.5) + "m)");
            }
        }
        
        System.out.println("  ✓ Total waypoints generados: " + waypoints.size());

        // Limitar a máximo 8 waypoints (aumentado de 5)
        if (waypoints.size() > 8) {
            waypoints = waypoints.subList(0, 8);
            System.out.println("  ⚠️ Limitado a 8 waypoints");
        }
        
        return waypoints;
    }
    
    private double getMinDistanceToDangers(GeoPoint point, List<CrimeAlert> alerts) {
        double minDistance = Double.MAX_VALUE;
        for (CrimeAlert alert : alerts) {
            if (alert.location != null) {
                double dist = calculateDistance(point, alert.location);
                minDistance = Math.min(minDistance, dist);
            }
        }
        return minDistance;
    }
    
    private List<RouteInfo> buildRouteWithWaypoints(GeoPoint start, GeoPoint end, List<GeoPoint> waypoints) throws IOException, JSONException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://graphhopper.com/api/1/route?");
        
        // Punto de origen
        urlBuilder.append("point=").append(start.getLatitude()).append(",").append(start.getLongitude());
        
        // Waypoints intermedios
        for (GeoPoint waypoint : waypoints) {
            urlBuilder.append("&point=").append(waypoint.getLatitude()).append(",").append(waypoint.getLongitude());
        }
        
        // Punto de destino
        urlBuilder.append("&point=").append(end.getLatitude()).append(",").append(end.getLongitude());
        
        // Parámetros
        String vehicle = vehicleMode ? "car" : "foot";
        urlBuilder.append("&vehicle=").append(vehicle)
                  .append("&key=").append(GRAPHHOPPER_API_KEY)
                  .append("&points_encoded=true");
        
        String urlString = urlBuilder.toString();
        
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            
            JSONObject jsonObject = new JSONObject(result.toString());
            JSONArray paths = jsonObject.getJSONArray("paths");
            List<RouteInfo> routes = new ArrayList<>();
            
            for (int i = 0; i < paths.length(); i++) {
                JSONObject path = paths.getJSONObject(i);
                String encodedPolyline = path.getString("points");
                List<GeoPoint> points = decodePolyline(encodedPolyline);
                long time = path.getLong("time");
                double distance = path.getDouble("distance");
                routes.add(new RouteInfo(points, time, distance));
                
                System.out.println("✅ Ruta segura generada: " + 
                    String.format("%.0f", distance) + "m, " + 
                    (time / 60000) + " min");
            }
            
            return routes;
            
        } catch (Exception e) {
            System.out.println("❌ Error generando ruta con waypoints: " + e.getMessage());
            return getRoutes(start, end);
        }
    }

    // MÉTODO DEPRECADO - Ya no se usa, ahora evaluamos rutas alternativas de GraphHopper
    private List<GeoPoint> generateSafeWaypoints(GeoPoint start, GeoPoint end) {
        return new ArrayList<>();
    }

    // MÉTODO DEPRECADO - Ya no se usa para rutas, solo para verificación de puntos individuales
    private boolean isPointNearDanger(GeoPoint point) {
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                double dangerRadius;
                switch (crime.severity) {
                    case 1: dangerRadius = 40; break;
                    case 2: dangerRadius = 80; break;
                    case 3: dangerRadius = 180; break;
                    case 4: dangerRadius = 250; break;
                    default: dangerRadius = 100; break;
                }
                
                double distance = calculateDistance(point, crime.location);
                if (distance <= dangerRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    // MÉTODO DEPRECADO - Ya no se usa
    private GeoPoint findSaferNearbyPoint(GeoPoint dangerousPoint) {
        return null;
    }

    private List<RouteInfo> filterSafeRoutes(List<RouteInfo> routes) {
        // Evaluar cada ruta basado en su proximidad a zonas de crimen
        List<RouteWithSafety> routesWithSafety = new ArrayList<>();
        
        for (RouteInfo route : routes) {
            double safetyScore = calculateRouteSafetyScore(route);
            routesWithSafety.add(new RouteWithSafety(route, safetyScore));
            // Debug
            System.out.println("filterSafeRoutes - Ruta con riesgo: " + safetyScore + ", Distancia: " + route.distanceInMeters + "m");
        }
        
        // Ordenar por puntuación de seguridad (menor puntuación = más seguro)
        routesWithSafety.sort((r1, r2) -> Double.compare(r1.safetyScore, r2.safetyScore));
        
        // Calcular umbral dinámico basado en las rutas disponibles
        List<RouteInfo> safeRoutes = new ArrayList<>();
        
        if (!routesWithSafety.isEmpty()) {
            double minRisk = routesWithSafety.get(0).safetyScore;
            double maxRisk = routesWithSafety.get(routesWithSafety.size() - 1).safetyScore;
            
            // Si hay diferencia significativa entre la más segura y la más peligrosa
            if (maxRisk - minRisk > 20.0) {
                // Usar umbral dinámico: riesgo mínimo + 30% de la diferencia
                double dynamicThreshold = minRisk + (maxRisk - minRisk) * 0.3;
                
                for (RouteWithSafety routeWithSafety : routesWithSafety) {
                    if (routeWithSafety.safetyScore <= dynamicThreshold) {
                        safeRoutes.add(routeWithSafety.route);
                    }
                }
                
                System.out.println("Umbral dinámico: " + dynamicThreshold + " (min: " + minRisk + ", max: " + maxRisk + ")");
            }
            
            // Si no hay rutas que cumplan el umbral dinámico, o la diferencia es pequeña,
            // devolver solo la más segura
            if (safeRoutes.isEmpty()) {
                safeRoutes.add(routesWithSafety.get(0).route);
                System.out.println("Usando solo la ruta más segura disponible con riesgo: " + routesWithSafety.get(0).safetyScore);
            }
        }
        
        return safeRoutes;
    }

    private List<RouteInfo> findSafestRoute(List<RouteInfo> allRoutes) {
        if (allRoutes.isEmpty()) {
            return new ArrayList<>();
        }

        RouteInfo safestRoute = null;
        double lowestRiskScore = Double.MAX_VALUE;

        // Evaluar cada ruta y encontrar la que tenga el menor riesgo
        for (RouteInfo route : allRoutes) {
            double riskScore = calculateRouteSafetyScore(route);
            
            // Log para debug
            System.out.println("Ruta evaluada - Riesgo: " + riskScore + ", Distancia: " + route.distanceInMeters + "m, Tiempo: " + route.timeInMillis/1000 + "s");
            
            if (riskScore < lowestRiskScore) {
                lowestRiskScore = riskScore;
                safestRoute = route;
            }
        }

        List<RouteInfo> result = new ArrayList<>();
        if (safestRoute != null) {
            result.add(safestRoute);
            System.out.println("Ruta más segura seleccionada con riesgo: " + lowestRiskScore);
        }
        
        return result;
    }

    private double calculateRouteSafetyScore(RouteInfo route) {
        double totalRisk = 0.0;
        int sampledPoints = 0;
        
        // Evaluar cada 10 puntos de la ruta para eficiencia
        for (int i = 0; i < route.points.size(); i += 10) {
            GeoPoint point = route.points.get(i);
            double pointRisk = calculatePointRisk(point);
            totalRisk += pointRisk;
            sampledPoints++;
        }
        
        // También evaluar el punto final
        if (!route.points.isEmpty()) {
            GeoPoint lastPoint = route.points.get(route.points.size() - 1);
            totalRisk += calculatePointRisk(lastPoint);
            sampledPoints++;
        }
        
        return sampledPoints > 0 ? totalRisk / sampledPoints : 0.0;
    }

    private double calculatePointRisk(GeoPoint point) {
        double totalRisk = 0.0;
        
        // **CORREGIDO**: Riesgo por alertas de crimen - SOLO crímenes CONFIRMADOS y relevantes al modo de transporte
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                // CORRECCIÓN: Solo considerar crímenes CONFIRMADOS para rutas
                if (!"CONFIRMADO".equals(crime.status)) {
                    continue; // Saltar crímenes no confirmados
                }
                
                // CORRECCIÓN: Solo considerar crímenes relevantes al modo de transporte
                if (vehicleMode) {
                    // Modo vehículo: solo crímenes de propiedad/vehículos
                    if (!"Delitos contra la propiedad".equals(crime.category)) {
                        continue;
                    }
                } else {
                    // Modo transeúnte: solo crímenes contra personas
                    if (!"Delitos contra las personas".equals(crime.category)) {
                        continue;
                    }
                }
                
                double distance = calculateDistance(point, crime.location);
                
                // Radio de influencia ajustado según gravedad y modo de transporte
                double influenceRadius;
                if (vehicleMode) {
                    // Vehículos: radios más amplios
                    influenceRadius = Math.min(40 + (crime.severity * 53.33), 200);
                } else {
                    // Peatones: radios más precisos y menores
                    influenceRadius = Math.min(25 + (crime.severity * 30), 120);
                }

                // Riesgo decae exponencialmente con la distancia
                if (distance <= influenceRadius) {
                    // Factor base de riesgo según gravedad
                    double severityWeight = crime.severity * 12.5;
                    
                    // Ya solo consideramos crímenes confirmados, no necesitamos multiplicador
                    
                    double riskFactor;
                    if (distance <= 100) {
                        // Muy alto riesgo si está muy cerca
                        riskFactor = severityWeight * 1.5;
                    } else if (distance <= 200) {
                        // Alto riesgo
                        riskFactor = severityWeight * Math.exp(-distance / 50.0);
                    } else {
                        // Riesgo moderado que decae exponencialmente
                        riskFactor = severityWeight * 0.5 * Math.exp(-distance / 100.0);
                    }
                    totalRisk += riskFactor;
                }
            }
        }
        
        // Riesgo adicional por zonas de peligro (polígonos)
        for (Polygon dangerZone : dangerZones) {
            if (isPointInPolygon(point, dangerZone)) {
                // Si el punto está dentro de una zona de peligro, agregar riesgo alto
                totalRisk += 100.0; // Riesgo muy alto por estar en zona de peligro
            } else {
                // Calcular riesgo por proximidad a la zona de peligro
                double distanceToZone = getDistanceToPolygon(point, dangerZone);
                if (distanceToZone <= 200) { // 200 metros de proximidad a zona de peligro
                    double proximityRisk = 25.0 * Math.exp(-distanceToZone / 50.0);
                    totalRisk += proximityRisk;
                }
            }
        }
        
        return totalRisk;
    }

    private boolean isPointInPolygon(GeoPoint point, Polygon polygon) {
        // Usar el método contains del polígono de OSMDroid si está disponible
        // Como alternativa, implementar ray casting algorithm
        List<GeoPoint> points = polygon.getPoints();
        if (points.size() < 3) return false;
        
        boolean inside = false;
        int j = points.size() - 1;
        
        for (int i = 0; i < points.size(); i++) {
            double xi = points.get(i).getLatitude();
            double yi = points.get(i).getLongitude();
            double xj = points.get(j).getLatitude();
            double yj = points.get(j).getLongitude();
            
            if (((yi > point.getLongitude()) != (yj > point.getLongitude())) &&
                (point.getLatitude() < (xj - xi) * (point.getLongitude() - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
            j = i;
        }
        
        return inside;
    }

    private double getDistanceToPolygon(GeoPoint point, Polygon polygon) {
        double minDistance = Double.MAX_VALUE;
        List<GeoPoint> points = polygon.getPoints();
        
        // Calcular distancia al punto más cercano del polígono
        for (GeoPoint polygonPoint : points) {
            double distance = calculateDistance(point, polygonPoint);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return minDistance;
    }

    // Clase auxiliar para asociar rutas con su puntuación de seguridad
    private static class RouteWithSafety {
        final RouteInfo route;
        final double safetyScore;

        RouteWithSafety(RouteInfo route, double safetyScore) {
            this.route = route;
            this.safetyScore = safetyScore;
        }
    }

    private void drawRoutes(List<RouteInfo> routes, GeoPoint start, GeoPoint end, boolean isSafeRoute) {
        clearRoute();
        List<GeoPoint> allPoints = new ArrayList<>();

        // ★★★ CAMBIO IMPORTANTE: PREPARAMOS LAS LÍNEAS PERO NO LAS DIBUJAMOS AÚN ★★★
        for (int i = 0; i < routes.size(); i++) {
            RouteInfo routeInfo = routes.get(i);
            allPoints.addAll(routeInfo.points); // Juntamos todos los puntos para el zoom
        }

        // Llamamos a selectRoute para que se encargue del dibujado inicial
        if (!routes.isEmpty()) {
            selectRoute(0, routes, isSafeRoute);
        }

        originMarker = new Marker(map);
        originMarker.setPosition(start);
        originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_origin_marker));
        originMarker.setTitle("Inicio");
        map.getOverlays().add(originMarker);

        destinationMarker = new Marker(map);
        destinationMarker.setPosition(end);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_destination_marker));
        destinationMarker.setTitle("Destino");
        map.getOverlays().add(destinationMarker);

        if (!allPoints.isEmpty()) {
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(allPoints);
            map.zoomToBoundingBox(boundingBox, true, 150);
        }
        map.invalidate();
    }

    // ★★★ MÉTODO COMPLETAMENTE NUEVO PARA DIBUJAR CON BORDES ★★★
    private void selectRoute(int index, List<RouteInfo> routes, boolean isSafeRoute) {
        // Primero, borramos solo las rutas viejas para poder redibujarlas
        map.getOverlays().removeAll(routeOverlays);
        routeOverlays.clear();

        // Definir colores según el tipo de ruta
        String selectedMainColor, selectedBorderColor, alternativeColor;
        
        if (isSafeRoute) {
            selectedMainColor = "#4CAF50"; // Verde para rutas seguras
            selectedBorderColor = "#2E7D32"; // Verde oscuro
            alternativeColor = "#81C784"; // Verde claro para alternativas
        } else {
            selectedMainColor = "#4285F4"; // Azul para rutas normales
            selectedBorderColor = "#0D47A1"; // Azul oscuro
            alternativeColor = "#8A8A8A"; // Gris para alternativas
        }

        // Dibujamos las rutas alternativas (no seleccionadas) primero
        for (int i = 0; i < routes.size(); i++) {
            if (i != index) {
                Polyline routeOverlay = new Polyline();
                routeOverlay.setPoints(routes.get(i).points);
                routeOverlay.setColor(Color.parseColor(alternativeColor));
                routeOverlay.setWidth(14f);
                final int routeIndex = i;
                routeOverlay.setOnClickListener((polyline, mapView, eventPos) -> {
                    selectRoute(routeIndex, routes, isSafeRoute);
                    return true;
                });
                map.getOverlays().add(routeOverlay);
                routeOverlays.add(routeOverlay);
            }
        }

        // Ahora, dibujamos la ruta seleccionada AL FINAL para que quede por encima
        if (index < routes.size()) {
            // BORDE: Línea ancha y oscura por debajo
            Polyline routeBorder = new Polyline();
            routeBorder.setPoints(routes.get(index).points);
            routeBorder.setColor(Color.parseColor(selectedBorderColor));
            routeBorder.setWidth(22f);
            final int routeIndex = index;
            routeBorder.setOnClickListener((polyline, mapView, eventPos) -> {
                selectRoute(routeIndex, routes, isSafeRoute); // El borde también es clickeable
                return true;
            });
            map.getOverlays().add(routeBorder);
            routeOverlays.add(routeBorder);

            // RELLENO: Línea brillante por encima del borde
            Polyline routeFill = new Polyline();
            routeFill.setPoints(routes.get(index).points);
            routeFill.setColor(Color.parseColor(selectedMainColor));
            routeFill.setWidth(16f);
            routeFill.setOnClickListener((polyline, mapView, eventPos) -> {
                selectRoute(routeIndex, routes, isSafeRoute);
                return true;
            });
            map.getOverlays().add(routeFill);
            routeOverlays.add(routeFill);
        }

        // Actualizamos el panel de información
        RouteInfo selectedRoute = routes.get(index);
        String routeTypeIndicator = isSafeRoute ? "🛡️ " : "";
        String info = String.format(Locale.getDefault(), "%s%s (%s)",
                routeTypeIndicator,
                formatDuration(selectedRoute.timeInMillis),
                formatDistance(selectedRoute.distanceInMeters)
        );
        // routeInfoText.setText(info); // COMENTADO: Ya no se usa el texto simple
        searchLayout.setVisibility(View.GONE);
        routeInfoLayout.setVisibility(View.VISIBLE);
        map.invalidate();
    }

    // Sobrecarga del método para mantener compatibilidad con rutas normales
    private void selectRoute(int index, List<RouteInfo> routes) {
        selectRoute(index, routes, false);
    }


    private String formatDuration(long millis) {
        long minutes = (millis / 1000) / 60;
        return minutes + " min";
    }

    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
        }
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new GeoPoint((double) lat / 1E5, (double) lng / 1E5));
        }
        return poly;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    
    private void setDestinationFromMapTap(GeoPoint point) {
        // Geocodificar inversamente para obtener la dirección
        new Thread(() -> {
            try {
                // Usar Nominatim para reverse geocoding
                String urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + 
                    point.getLatitude() + "&lon=" + point.getLongitude() + "&zoom=18&addressdetails=1";
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", getPackageName());
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                
                JSONObject jsonObject = new JSONObject(result.toString());
                String displayName = jsonObject.getString("display_name");
                
                // Extraer dirección más corta
                String address = displayName;
                if (jsonObject.has("address")) {
                    JSONObject addressObj = jsonObject.getJSONObject("address");
                    StringBuilder shortAddress = new StringBuilder();
                    
                    if (addressObj.has("road")) {
                        shortAddress.append(addressObj.getString("road"));
                        if (addressObj.has("house_number")) {
                            shortAddress.append(" ").append(addressObj.getString("house_number"));
                        }
                        shortAddress.append(", Buenos Aires, Argentina");
                        address = shortAddress.toString();
                    }
                }
                
                final String finalAddress = address;
                
                runOnUiThread(() -> {
                    destinationEditText.setText(finalAddress);
                    Toast.makeText(MainActivity.this, "📍 Destino: " + finalAddress, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Si falla el reverse geocoding, usar las coordenadas directamente
                    String coords = String.format(Locale.US, "%.6f, %.6f", point.getLatitude(), point.getLongitude());
                    destinationEditText.setText(coords);
                    Toast.makeText(MainActivity.this, "📍 Destino establecido en: " + coords, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void exportToUber() {
        String origin = originEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        
        if (origin.isEmpty()) {
            origin = "Av. Santa Fe 995, Buenos Aires, Argentina";
        }
        
        if (destination.isEmpty()) {
            Toast.makeText(this, "No hay destino definido", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Construir URL de Uber con origen y destino
            String uberUrl = "uber://?action=setPickup&pickup=my_location&dropoff[formatted_address]=" + 
                           URLEncoder.encode(destination, "UTF-8");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(uberUrl));
            
            // Verificar si la app de Uber está instalada
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Si no está instalada, abrir en el navegador
                String webUrl = "https://m.uber.com/looking?drop%5B0%5D%5Baddress%5D=" + 
                              URLEncoder.encode(destination, "UTF-8");
                intent.setData(android.net.Uri.parse(webUrl));
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir Uber", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void exportToPedidosYa() {
        String origin = originEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        
        if (origin.isEmpty()) {
            origin = "Av. Santa Fe 995, Buenos Aires, Argentina";
        }
        
        if (destination.isEmpty()) {
            Toast.makeText(this, "No hay destino definido", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Construir URL de Pedidos Ya
            // Pedidos Ya usa un formato similar a otros servicios de delivery
            String pedidosYaUrl = "pedidosya://rides?destination=" + URLEncoder.encode(destination, "UTF-8");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(pedidosYaUrl));
            
            // Verificar si la app está instalada
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Si no está instalada, abrir en el navegador
                String webUrl = "https://www.pedidosya.com.ar/envios";
                intent.setData(android.net.Uri.parse(webUrl));
                startActivity(intent);
                Toast.makeText(this, "Pedidos Ya no está instalado. Abriendo página web...", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir Pedidos Ya", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadCrimesFromBackend() {
        HashMap<String, String > request = new HashMap<>();
        // Cargar crímenes desde el backend
        ApiClient.getService().getCrimenes(request).enqueue(new Callback<List<CrimeDto>>() {
            @Override
            public void onResponse(Call<List<CrimeDto>> call, Response<List<CrimeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CrimeDto> crimesFromBackend = response.body();

                    // Limpiar alertas actuales
                    crimeAlerts.clear();

                    // Convertir CrimeDto a CrimeAlert
                    for (CrimeDto crime : crimesFromBackend) {
                        try {
                            // Validar que los campos obligatorios no sean null
                            if (crime.category == null ) {
                                android.util.Log.w("MainActivity", "Crimen con type null o vacío, ignorando...");
                                continue; // Saltar este crimen
                            }

                            if (crime.description == null) {
                                crime.description = "Sin descripción";
                            }

                            if (crime.address == null) {
                                crime.address = "Ubicación desconocida";
                            }

                            // **CORREGIDO**: Usar directamente los valores del backend sin lógica de palabras
                            String subType = crime.category.getCode();
                            int severity = crime.category.getValue(); // Severidad viene directa del backend
                            
                            // Mapear códigos exactos del backend a categorías de la UI
                            String category;
                            String crimeCode = crime.category.getCode();
                            
                            // **CORRECCIÓN BACKEND**: Usar los códigos reales del backend (db.go líneas 25-32)
                            // El backend ahora usa Type para clasificar: "CONTRA_LA_PROPIEDAD" o "CONTRA_LAS_PERSONAS"
                            String backendType = crime.category.getType();
                            switch (backendType) {
                                case "CONTRA_LA_PROPIEDAD":
                                    category = "Delitos contra la propiedad";
                                    break;
                                case "CONTRA_LAS_PERSONAS":
                                    category = "Delitos contra las personas";
                                    break;
                                default:
                                    category = "Otros delitos";
                                    break;
                            }

                            // **SIMPLIFICADO**: Mapeo directo basado en Type del backend
                            String crimeType;
                            
                            if ("CONTRA_LA_PROPIEDAD".equals(backendType)) {
                                // TODOS los delitos contra la propiedad son de vehículos
                                crimeType = "Robo de vehículos";
                            } else {
                                // TODOS los otros (CONTRA_LAS_PERSONAS) son de transeúntes
                                crimeType = "Crimen en vía pública";
                            }
                            
                            System.out.println("🗂️ MAPEO BACKEND -> UI (SIMPLIFICADO):");
                            System.out.println("   ├─ Código backend: '" + crimeCode + "'");
                            System.out.println("   ├─ Tipo backend: '" + backendType + "'");
                            System.out.println("   ├─ Severidad backend: " + severity);
                            System.out.println("   ├─ Status: '" + crime.status + "'");
                            System.out.println("   ├─ Categoría UI: '" + category + "'");
                            System.out.println("   └─ Filtro UI: '" + crimeType + "' (Propiedad=Vehículos, Personas=Transeúntes)");
                            
                            // **CORRECCIÓN**: Crear título y estado basado en el status del backend
                            String title = crime.description;
                            String timeStatus;
                            
                            switch (crime.status) {
                                case "CONFIRMADO":
                                    timeStatus = "Confirmado (" + crime.verification + " verificaciones)";
                                    break;
                                case "PENDIENTE":
                                    timeStatus = "⚠️ Pendiente de verificación (" + crime.verification + "/3)";
                                    break;
                                case "VALIDACION_COMUNIDAD":
                                    timeStatus = "🔄 En validación comunitaria (" + crime.verification + "/3)";
                                    break;
                                default:
                                    timeStatus = crime.status + " (" + crime.verification + " verificaciones)";
                                    break;
                            }
                            
                            // Crear CrimeAlert desde CrimeDto
                            CrimeAlert alert = new CrimeAlert(
                                crime.id,
                                title,
                                crime.description,
                                crime.address,
                                timeStatus,
                                crimeType, // Usar el crimeType mapeado correctamente
                                category,
                                subType,
                                severity,
                                crime.reporter,
                                crime.verification,
                                crime.status
                            );

                            // Establecer ubicación directamente desde el backend
                            alert.location = new GeoPoint(Double.parseDouble(crime.latitude), Double.parseDouble( crime.longitude));

                            crimeAlerts.add(alert);
                        } catch (Exception e) {
                            android.util.Log.e("MainActivity", "Error procesando crimen: " + e.getMessage());
                            e.printStackTrace();
                            // Continuar con el siguiente crimen
                        }
                    }

                    // Agregar marcadores al mapa
                    addCrimeAlertsToMap();

                    Toast.makeText(MainActivity.this,
                            "✅ " + crimeAlerts.size() + " incidentes cargados",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "⚠️ Error al cargar incidentes. Usando datos de ejemplo.",
                            Toast.LENGTH_SHORT).show();
                    
                    // **MEJORADO**: Fallback a datos hardcodeados más completos si el backend falla
                    setupHardcodedCrimeAlerts();
                    addCrimeAlertsToMap();
                }
            }

            @Override
            public void onFailure(Call<List<CrimeDto>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "⚠️ Sin conexión. Usando datos de ejemplo.",
                        Toast.LENGTH_SHORT).show();
                
                // **MEJORADO**: Fallback a datos hardcodeados más completos si hay error de conexión
                setupHardcodedCrimeAlerts();
                addCrimeAlertsToMap();
            }
        });
    }



    private void addCrimeAlertsToMap() {
        System.out.println("🗺️ === AGREGANDO CRÍMENES AL MAPA ===");
        System.out.println("📊 Total de crímenes: " + crimeAlerts.size());
        System.out.println("🔧 Filtros activos - Calles: " + showStreetCrime + ", Vehículos: " + showVehicleCrime);
        
        // **CORRECCIÓN CRÍTICA**: Limpiar marcadores existentes primero para evitar bug de filtros
        clearCrimeAlertMarkersFromMap();
        
        // PRIMERO: Crear zonas de calor (círculos) ANTES de los marcadores
        // Esto hace que los círculos estén debajo y los marcadores encima
        createDangerZones();

        // SEGUNDO: Agregar los marcadores ENCIMA de las zonas usando el método mejorado
        int markersAdded = 0;
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location != null && shouldShowCrime(alert)) { // Solo agregar si tiene ubicación Y pasa el filtro
                System.out.println("🎯 Procesando: " + alert.title + " (tipo: " + alert.crimeType + ", status: " + alert.status + ")");
                
                // **CORRECCIÓN**: Determinar si el crimen está activo basándose en su estado
                boolean isActive = "CONFIRMADO".equals(alert.status);
                
                // Usar el método mejorado que maneja diferentes tipos de marcadores
                Marker marker = createCrimeMarker(alert, isActive);
                map.getOverlays().add(marker);
                crimeAlertMarkers.add(marker);
                
                // Agregar animación de rebote al marcador
                startCrimeAlertAnimation(marker);
                markersAdded++;
            }
        }
        System.out.println("✅ Marcadores agregados al mapa: " + markersAdded + "/" + crimeAlerts.size());
        
        map.invalidate();

        // Centrar el mapa en la zona de las alertas si hay alertas
        if (!crimeAlerts.isEmpty()) {
            centerMapOnAlerts();
        }
    }
    
    private void centerMapOnAlerts() {
        // Crear una lista con todas las ubicaciones de las alertas para centrar el mapa
        List<GeoPoint> alertLocations = new ArrayList<>();
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location != null) {
                alertLocations.add(alert.location);
            }
        }
        
        if (!alertLocations.isEmpty()) {
            // Si tenemos ubicaciones de alertas, centrar el mapa en ellas
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(alertLocations);
            map.zoomToBoundingBox(boundingBox, true, 200); // 200 es el padding
        } else {
            // Si no se pudieron geocodificar, usar la ubicación por defecto en Corrientes
            IMapController mapController = map.getController();
            mapController.setZoom(17.0);
            mapController.setCenter(new GeoPoint(-34.6035, -58.3794)); // Av. Corrientes aprox.
        }
    }

    private void showCrimeAlertDialog(CrimeAlert alert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Personalizar título según la categoría
        String titleIcon = "Delitos contra la propiedad".equals(alert.category) ? "🚗" : "⚠️";
        builder.setTitle(titleIcon + " " + alert.title);
        
        // Emoji de gravedad
        String severityEmoji;
        switch (alert.severity) {
            case 4: severityEmoji = "🔴"; break;
            case 3: severityEmoji = "🟠"; break;
            case 2: severityEmoji = "🟡"; break;
            case 1: severityEmoji = "🟢"; break;
            default: severityEmoji = "⚪"; break;
        }

        String message = "📂 Categoría: " + alert.category + "\n\n" +
                        "🏷️ Tipo: " + alert.subType + "\n\n" +
                        severityEmoji + " Gravedad: " + alert.getSeverityText() + " (" + alert.severity + "/4)\n\n" +
                        "📍 Ubicación: " + alert.address + "\n\n" +
                        "🕒 Status: " + alert.status + "\n\n" +
                        "📝 Detalles: " + alert.description + "\n\n" +
                        "✓ Verificaciones: " + alert.verification + "\n\n";

        // Mensaje de precaución específico según la categoría y gravedad
        if ("Delitos contra la propiedad".equals(alert.category)) {
            message += "🚨 Recomendación: ";
            if (alert.severity >= 3) {
                message += "PELIGRO ALTO - Evite estacionar vehículos en esta zona. " +
                          "Si debe hacerlo, use sistemas de seguridad múltiples y estacione en lugares vigilados.";
            } else {
                message += "Use sistemas de seguridad adicionales y evite dejar objetos de valor a la vista.";
            }
        } else {
            message += "⚠️ Recomendación: ";
            if (alert.severity >= 3) {
                message += "PELIGRO ALTO - Se recomienda evitar esta zona. Si debe transitar, hágalo acompañado " +
                          "y en horarios diurnos. Mantenga alerta máxima.";
            } else if (alert.severity == 2) {
                message += "Transite con precaución, especialmente en horarios nocturnos. " +
                          "Manténgase alerta y evite mostrar objetos de valor.";
            } else {
                message += "Mantenga precauciones básicas. Esté atento a su entorno.";
            }
        }
        
        builder.setMessage(message);

        // Verificar si el usuario actual es el que reportó este crimen
        String currentUserEmail = UserSession.getCurrentUserMail();
        boolean isOwnReport = currentUserEmail != null && currentUserEmail.equals(alert.reporter);

        // Botón de verificación (solo si NO es el propio reporte del usuario)
        if (!isOwnReport && alert.id > 0) { // Solo para reportes del backend (id positivos)
            builder.setNeutralButton("✓ Verificar Reporte", (dialog, which) -> {
                verifyReport(alert);
            });
        }

        // Botón de cerrar
        builder.setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss());
        builder.setIcon(R.drawable.ic_alert_warning);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void verifyReport(CrimeAlert alert) {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) {
            Toast.makeText(this, "Error: No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear request con el ID del reporte y el email del usuario
        CrimeVerifyRequest request = new CrimeVerifyRequest(String.valueOf(alert.id), userEmail);

        // Llamar al endpoint de verificación
        ApiClient.getService().verificarCrimen(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this,
                                "✅ Reporte verificado exitosamente",
                                Toast.LENGTH_SHORT).show();

                        // Recargar los reportes para actualizar el contador de verificaciones
                        loadCrimesFromBackend();
                        
                        // Verificar si ganó puntos o logros (solo después de verificación exitosa)
                        checkUserRewardsAfterVerification();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "⚠️ No se pudo verificar el reporte",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "❌ Error de conexión: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Crea un marcador de crimen mejorado con diferentes estilos según severidad y categoría
     * Restaura la funcionalidad completa de la implementación anterior
     */
    private Marker createCrimeMarker(CrimeAlert alert, boolean isActive) {
        System.out.println("🔧 Creando marcador mejorado: " + alert.title + " | Activo: " + isActive + " | Severidad: " + alert.severity);
        
        Marker marker = new Marker(map);
        marker.setPosition(alert.location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        if (isActive) {
            System.out.println("  ➡️ Marcador ACTIVO: ícono con tamaño basado en severidad");
            
            // Título con emoji según categoría
            String categoryEmoji = "Delitos contra la propiedad".equals(alert.category) ? "🚗" : "⚠️";
            marker.setTitle(categoryEmoji + " " + alert.title);
            marker.setSnippet(alert.timeAgo + " - " + alert.crimeType);
            marker.setSubDescription(alert.description + "\n" + alert.timeAgo + "\nSeveridad: " + alert.getSeverityText());
            
            // Seleccionar ícono base
            Drawable alertIcon = ContextCompat.getDrawable(this, R.drawable.ic_alert_warning);
            
            if (alertIcon != null) {
                // **ACTUALIZADO**: Colores según el nuevo esquema
                if ("Delitos contra la propiedad".equals(alert.category)) {
                    // Delitos contra la propiedad (vehículos) - Color violeta
                    int baseColor = Color.parseColor("#9C27B0"); // Violeta base
                    int adjustedColor = adjustColorBySeverity(baseColor, alert.severity);
                    alertIcon.setTint(adjustedColor);
                } else {
                    // Delitos contra las personas (transeúntes) - Color ROJO como solicitado
                    int baseColor = Color.parseColor("#F44336"); // Rojo base
                    int adjustedColor = adjustColorBySeverity(baseColor, alert.severity);
                    alertIcon.setTint(adjustedColor);
                }
                
                // **CLAVE: Ajustar el tamaño del ícono según severidad**
                int iconSize = getIconSizeBySeverity(alert.severity);
                alertIcon.setBounds(0, 0, iconSize, iconSize);
            }
            
            marker.setIcon(alertIcon);
            
            // Click listener para mostrar diálogo detallado
            marker.setOnMarkerClickListener((marker1, mapView) -> {
                showCrimeAlertDialog(alert);
                return true;
            });
        } else {
            System.out.println("  ➡️ Marcador INACTIVO: ícono diferente");
            
            // Marcador para crímenes inactivos/pendientes
            marker.setTitle("📍 " + alert.title + " (Pendiente)");
            marker.setSnippet(alert.timeAgo + " - Verificaciones: " + alert.verification);
            marker.setSubDescription(alert.description + "\n" + alert.timeAgo + "\n⚪ Pendiente de verificación");
            
            // Usar ícono diferente para inactivos si está disponible
            Drawable inactiveIcon = ContextCompat.getDrawable(this, R.drawable.ic_inactive_crime);
            if (inactiveIcon == null) {
                // Fallback: usar el ícono normal pero con color diferente
                inactiveIcon = ContextCompat.getDrawable(this, R.drawable.ic_alert_warning);
                if (inactiveIcon != null) {
                    inactiveIcon.setTint(Color.parseColor("#FFC107")); // Amarillo para pendientes
                }
            }
            marker.setIcon(inactiveIcon);
            
            // Click listener para crímenes inactivos
            marker.setOnMarkerClickListener((marker1, mapView) -> {
                showCrimeAlertDialog(alert); // Usar el mismo diálogo mejorado
                return true;
            });
        }
        
        return marker;
    }

    /**
     * Ajusta la intensidad del color según la severidad del crimen
     */
    private int adjustColorBySeverity(int baseColor, int severity) {
        float alpha = 0.6f + (severity * 0.1f); // De 0.7 a 1.0 según severidad
        alpha = Math.min(alpha, 1.0f);
        
        int red = (int) (Color.red(baseColor) * alpha);
        int green = (int) (Color.green(baseColor) * alpha);
        int blue = (int) (Color.blue(baseColor) * alpha);
        
        return Color.rgb(red, green, blue);
    }

    /**
     * Devuelve el tamaño del ícono según la severidad (en píxeles)
     */
    private int getIconSizeBySeverity(int severity) {
        switch (severity) {
            case 1: return 48;  // Leve - Pequeño
            case 2: return 64;  // Moderado - Mediano
            case 3: return 80;  // Grave - Grande
            case 4: return 96;  // Muy Grave - Muy grande
            default: return 64; // Por defecto mediano
        }
    }

    // Método eliminado - ya no se usa el botón amarillo para mostrar/ocultar todas las zonas

    private void createDangerZones() {
        // Primero, limpiar zonas existentes
        hideDangerZones();
        
        // **NUEVO REQUERIMIENTO**: Solo crear círculos para crímenes CONFIRMADOS que pasan los filtros
        int circlesCreated = 0;
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null && shouldShowCrime(crime) && "CONFIRMADO".equals(crime.status)) {
                // Solo crímenes confirmados tienen círculo de peligro
                System.out.println("🔵 Creando círculo para: " + crime.title + " (CONFIRMADO, severidad " + crime.severity + ")");
                createDangerZone(crime.location, crime.severity);
                circlesCreated++;
            } else if (crime.location != null && shouldShowCrime(crime)) {
                System.out.println("⚪ Sin círculo para: " + crime.title + " (Status: " + crime.status + ")");
            }
        }
        System.out.println("📊 Total círculos creados: " + circlesCreated + "/" + crimeAlerts.size());
        
        map.invalidate();
    }

    private int countNearbyCrimes(GeoPoint location, double radiusInMeters) {
        int count = 0;
        
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                double distance = calculateDistance(location, crime.location);
                if (distance <= radiusInMeters) {
                    count++;
                }
            }
        }
        
        return count;
    }

    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        // Fórmula de Haversine para calcular distancia entre dos puntos geográficos
        double R = 6371000; // Radio de la Tierra en metros
        double lat1Rad = Math.toRadians(point1.getLatitude());
        double lat2Rad = Math.toRadians(point2.getLatitude());
        double deltaLatRad = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double deltaLonRad = Math.toRadians(point2.getLongitude() - point1.getLongitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private Map<String, List<CrimeAlert>> analyzeCrimeDensity() {
        Map<String, List<CrimeAlert>> crimesByArea = new HashMap<>();
        
        // Agrupar crímenes por áreas cercanas (aproximadamente cada 3-4 cuadras)
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                String areaKey = getAreaKey(crime.location);
                crimesByArea.computeIfAbsent(areaKey, k -> new ArrayList<>()).add(crime);
            }
        }
        
        return crimesByArea;
    }

    private String getAreaKey(GeoPoint location) {
        // Crear una clave de área basada en coordenadas redondeadas
        // Esto agrupa ubicaciones cercanas en la misma "zona"
        double latRounded = Math.round(location.getLatitude() * 1000.0) / 1000.0;
        double lonRounded = Math.round(location.getLongitude() * 1000.0) / 1000.0;
        return latRounded + "," + lonRounded;
    }

    private GeoPoint calculateAreaCenter(List<CrimeAlert> crimes) {
        double totalLat = 0;
        double totalLon = 0;
        int count = 0;
        
        for (CrimeAlert crime : crimes) {
            if (crime.location != null) {
                totalLat += crime.location.getLatitude();
                totalLon += crime.location.getLongitude();
                count++;
            }
        }

        if (count > 0) {
            return new GeoPoint(totalLat / count, totalLon / count);
        }
        return null;
    }

    private void createDangerZone(GeoPoint center, int severity) {
        if (center == null) return;

        // **RESTAURADO**: Usar EXACTAMENTE los valores de la implementación funcional
        int color;
        double radiusInMeters;
        int alpha;

        switch (severity) {
            case 1: // Leve
                color = Color.parseColor("#FFD700"); // Amarillo dorado
                radiusInMeters = 40; // Radio pequeño para crímenes leves
                alpha = 50; // Muy transparente
                break;
            case 2: // Moderado
                color = Color.parseColor("#FFA500"); // Naranja
                radiusInMeters = 80; // Radio medio-bajo
                alpha = 70; // Moderadamente visible
                break;
            case 3: // Grave
                color = Color.parseColor("#FF4444"); // Rojo más claro
                radiusInMeters = 180; // Radio grande
                alpha = 85; // Visible pero no excesivo
                break;
            case 4: // Muy Grave
                color = Color.parseColor("#CC0000"); // Rojo intenso pero no tan oscuro
                radiusInMeters = 250; // Radio muy grande para máxima gravedad
                alpha = 100; // Opaco pero no tanto como para ocultar el mapa
                break;
            default: // Fallback
                color = Color.parseColor("#FFA500"); // Naranja por defecto
                radiusInMeters = 80;
                alpha = 70;
                break;
        }
        
        // Limitar el alpha máximo para no ocultar demasiado el mapa
        alpha = Math.min(alpha, 100); // Reducido para mejor visibilidad del mapa
        
        // Crear círculo de peligro
        List<GeoPoint> circlePoints = createCirclePoints(center, radiusInMeters);
        
        Polygon dangerZone = new Polygon();
        dangerZone.setPoints(circlePoints);
        dangerZone.setFillColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
        dangerZone.setStrokeColor(Color.argb(Math.min(alpha + 50, 255), Color.red(color), Color.green(color), Color.blue(color)));
        dangerZone.setStrokeWidth(2f);
        
        map.getOverlays().add(dangerZone);
        dangerZones.add(dangerZone);
    }

    private List<GeoPoint> createCirclePoints(GeoPoint center, double radiusInMeters) {
        List<GeoPoint> points = new ArrayList<>();
        int numPoints = 50; // Más puntos para un círculo más suave
        
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            
            // Convertir metros a grados (aproximación)
            double deltaLat = radiusInMeters / 111000.0; // ~111km por grado de latitud
            double deltaLon = radiusInMeters / (111000.0 * Math.cos(Math.toRadians(center.getLatitude())));
            
            double lat = center.getLatitude() + deltaLat * Math.cos(angle);
            double lon = center.getLongitude() + deltaLon * Math.sin(angle);
            
            points.add(new GeoPoint(lat, lon));
        }
        
        return points;
    }

    private void hideDangerZones() {
        map.getOverlays().removeAll(dangerZones);
        dangerZones.clear();
        map.invalidate();
    }

    private void showReportCrimeDialogWithLocation(GeoPoint location, String address) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Reportar Crimen en Ubicación Seleccionada");

        // Inflar el layout del formulario
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_crime, null);
        builder.setView(dialogView);

        // Referencias a los campos del formulario
        EditText addressInput = dialogView.findViewById(R.id.address_input);
        EditText incidentDescriptionInput = dialogView.findViewById(R.id.incident_description_input);
        EditText timeInput = dialogView.findViewById(R.id.time_input);
        Button selectImageButton = dialogView.findViewById(R.id.select_image_button);
        TextView imageSelectedText = dialogView.findViewById(R.id.image_selected_text);
        android.widget.Spinner categorySpinner = dialogView.findViewById(R.id.category_spinner);
        android.widget.Spinner subtypeSpinner = dialogView.findViewById(R.id.subtype_spinner);
        TextView severityInfoText = dialogView.findViewById(R.id.severity_info_text);

        // PRELLENAR LA DIRECCIÓN con la ubicación seleccionada
        if (address != null && !address.isEmpty()) {
            addressInput.setText(address);
        } else {
            // Si no hay dirección, usar coordenadas
            String coords = String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
            addressInput.setText(coords);
        }

        // Deshabilitar edición de dirección (opcional - ya está seleccionada)
        // addressInput.setEnabled(false); // Comentado para permitir edición si el usuario quiere ajustar

        // **RESTAURADO**: Configurar categorías EXACTAMENTE como en la implementación funcional
        String[] categories = {"Delitos contra las personas", "Delitos contra la propiedad"};
        android.widget.ArrayAdapter<String> categoryAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(categoryAdapter);

        // **RESTAURADO**: Mapa de subtipos EXACTAMENTE como en la implementación funcional
        Map<String, Map<String, Integer>> subtypesByCategoryWithSeverity = new HashMap<>();

        // Delitos contra las personas
        Map<String, Integer> personCrimes = new HashMap<>();
        personCrimes.put("Homicidio", 4);
        personCrimes.put("Agresión grave", 3);
        personCrimes.put("Robo/Arrebato", 2);
        personCrimes.put("Hurto", 1);
        personCrimes.put("Agresión leve", 1);
        subtypesByCategoryWithSeverity.put("Delitos contra las personas", personCrimes);

        // Delitos contra la propiedad
        Map<String, Integer> propertyCrimes = new HashMap<>();
        propertyCrimes.put("Robo armado", 4);
        propertyCrimes.put("Robo vehículo estacionado", 3);
        propertyCrimes.put("Robo pertenencias de vehículo", 2);
        subtypesByCategoryWithSeverity.put("Delitos contra la propiedad", propertyCrimes);

        // Configurar listener para cambio de categoría
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                Map<String, Integer> subtypes = subtypesByCategoryWithSeverity.get(selectedCategory);

                String[] subtypeArray = subtypes.keySet().toArray(new String[0]);
                android.widget.ArrayAdapter<String> subtypeAdapter = new android.widget.ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_spinner_dropdown_item, subtypeArray);
                subtypeSpinner.setAdapter(subtypeAdapter);

                // Actualizar gravedad cuando cambia el subtipo
                subtypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> subParent, View subView, int subPosition, long subId) {
                        String selectedSubtype = subtypeArray[subPosition];
                        Integer severity = subtypes.get(selectedSubtype);

                        String severityEmoji;
                        String severityText;
                        int severityColor;

                        switch (severity) {
                            case 1:
                                severityEmoji = "🟢";
                                severityText = "Leve";
                                severityColor = Color.parseColor("#4CAF50");
                                break;
                            case 2:
                                severityEmoji = "🟡";
                                severityText = "Moderado";
                                severityColor = Color.parseColor("#FFC107");
                                break;
                            case 3:
                                severityEmoji = "🟠";
                                severityText = "Grave";
                                severityColor = Color.parseColor("#FF5722");
                                break;
                            case 4:
                                severityEmoji = "🔴";
                                severityText = "Muy Grave";
                                severityColor = Color.parseColor("#8B0000");
                                break;
                            default:
                                severityEmoji = "⚪";
                                severityText = "Desconocido";
                                severityColor = Color.GRAY;
                                break;
                        }

                        severityInfoText.setText(severityEmoji + " Gravedad: " + severityText + " (" + severity + "/4)");
                        severityInfoText.setTextColor(severityColor);
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });

                // Trigger inicial
                if (subtypeArray.length > 0) {
                    subtypeSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Simular selección de imagen
        selectImageButton.setOnClickListener(v -> {
            imageSelectedText.setText("✅ Imagen seleccionada: foto_evidencia.jpg");
            Toast.makeText(this, "Función de galería no implementada en la demo", Toast.LENGTH_SHORT).show();
        });

        // Trigger inicial de categoría
        categorySpinner.setSelection(0);

        builder.setPositiveButton("Enviar Reporte", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String userEmail = UserSession.getCurrentUserMail();
            if (userEmail == null) {
                Toast.makeText(MainActivity.this, "Error: No hay usuario en sesión", Toast.LENGTH_SHORT).show();
                return;
            }

            String addressText = addressInput.getText().toString().trim();
            String description = incidentDescriptionInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            String subtype = subtypeSpinner.getSelectedItem().toString();

            if (addressText.isEmpty() || description.isEmpty() || time.isEmpty()) {
                Toast.makeText(MainActivity.this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener gravedad
            Map<String, Integer> subtypes = subtypesByCategoryWithSeverity.get(category);
            int severity = subtypes.get(subtype);

            // Deshabilitar botón para evitar múltiples envíos
            positiveButton.setEnabled(false);
            positiveButton.setText("Enviando...");

            // Usar la ubicación que ya tenemos del long press
            new Thread(() -> {
                try {
                    // Crear request para el backend
                    CrimeCreateRequest request = new CrimeCreateRequest(
                            subtype,
                            description,
                            addressText,
                            String.valueOf(location.getLatitude()),
                            String.valueOf(location.getLongitude()),
                            userEmail,
                            time
                    );

                    // Enviar al backend
                    ApiClient.getService().crearCrimen(request).enqueue(new Callback<CrimeDto>() {
                        @Override
                        public void onResponse(Call<CrimeDto> call, Response<CrimeDto> response) {
                            if (response.isSuccessful()) {
                                runOnUiThread(() -> {
                                    dialog.dismiss();
                                    showReportSuccessDialog(category, subtype, severity);
                                    Toast.makeText(MainActivity.this,
                                            "✅ Reporte enviado exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                    loadCrimesFromBackend();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Error al enviar reporte (código: " + response.code() + ")",
                                            Toast.LENGTH_SHORT).show();
                                    positiveButton.setEnabled(true);
                                    positiveButton.setText("Enviar Reporte");
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<CrimeDto> call, Throwable t) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Error de conexión: " + t.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                positiveButton.setEnabled(true);
                                positiveButton.setText("Enviar Reporte");
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        positiveButton.setEnabled(true);
                        positiveButton.setText("Enviar Reporte");
                    });
                }
            }).start();
        });
    }

    private void showReportCrimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Reportar Crimen");
        
        // Inflar el layout del formulario
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_crime, null);
        builder.setView(dialogView);
        
        // Referencias a los campos del formulario
        EditText addressInput = dialogView.findViewById(R.id.address_input);
        EditText incidentDescriptionInput = dialogView.findViewById(R.id.incident_description_input);
        EditText timeInput = dialogView.findViewById(R.id.time_input);
        Button selectImageButton = dialogView.findViewById(R.id.select_image_button);
        TextView imageSelectedText = dialogView.findViewById(R.id.image_selected_text);
        android.widget.Spinner categorySpinner = dialogView.findViewById(R.id.category_spinner);
        android.widget.Spinner subtypeSpinner = dialogView.findViewById(R.id.subtype_spinner);
        TextView severityInfoText = dialogView.findViewById(R.id.severity_info_text);
        
        // **RESTAURADO**: Configurar categorías EXACTAMENTE como en la implementación funcional
        String[] categories = {"Delitos contra las personas", "Delitos contra la propiedad"};
        android.widget.ArrayAdapter<String> categoryAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(categoryAdapter);
        
        // **CORRECCIÓN BACKEND**: Mapeo UI → Códigos Backend con severidades reales
        Map<String, Map<String, Object[]>> subtypesByCategoryWithData = new HashMap<>();
        
        // Delitos contra las personas - mapear a códigos del backend
        Map<String, Object[]> personCrimes = new HashMap<>();
        personCrimes.put("Homicidio", new Object[]{"HOMICIDIO", 4});
        personCrimes.put("Agresión grave", new Object[]{"AGRESION_GRAVES", 3});
        personCrimes.put("Agresión leve", new Object[]{"AGRESION_LEVES", 2});
        personCrimes.put("Robo/Arrebato", new Object[]{"ROBO_ARREBATO", 2});
        personCrimes.put("Hurto", new Object[]{"HURTO", 1});
        subtypesByCategoryWithData.put("Delitos contra las personas", personCrimes);
        
        // Delitos contra la propiedad - mapear a códigos del backend
        Map<String, Object[]> propertyCrimes = new HashMap<>();
        propertyCrimes.put("Robo con violencia", new Object[]{"ROBO_CON_VIOLENCIA", 3});
        propertyCrimes.put("Robo de pertenencias", new Object[]{"ROBO_PERTENENCIAS", 2});
        propertyCrimes.put("Robo sin presencia", new Object[]{"ROBO_SIN_PRESENCIA", 3});
        subtypesByCategoryWithData.put("Delitos contra la propiedad", propertyCrimes);
        
        // Crear mapa solo de severidades para compatibilidad
        Map<String, Map<String, Integer>> subtypesByCategoryWithSeverity = new HashMap<>();
        for (Map.Entry<String, Map<String, Object[]>> entry : subtypesByCategoryWithData.entrySet()) {
            Map<String, Integer> severityMap = new HashMap<>();
            for (Map.Entry<String, Object[]> subEntry : entry.getValue().entrySet()) {
                severityMap.put(subEntry.getKey(), (Integer) subEntry.getValue()[1]);
            }
            subtypesByCategoryWithSeverity.put(entry.getKey(), severityMap);
        }
        
        // Configurar listener para cambio de categoría
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                Map<String, Integer> subtypes = subtypesByCategoryWithSeverity.get(selectedCategory);
                
                String[] subtypeArray = subtypes.keySet().toArray(new String[0]);
                android.widget.ArrayAdapter<String> subtypeAdapter = new android.widget.ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_spinner_dropdown_item, subtypeArray);
                subtypeSpinner.setAdapter(subtypeAdapter);
                
                // Actualizar gravedad cuando cambia el subtipo
                subtypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        String selectedSubtype = subtypeArray[position];
                        Integer severity = subtypes.get(selectedSubtype);
                        
                        String severityEmoji;
                        String severityText;
                        int severityColor;
                        
                        switch (severity) {
                            case 4:
                                severityEmoji = "🔴";
                                severityText = "Muy Grave";
                                severityColor = Color.parseColor("#8B0000");
                                break;
                            case 3:
                                severityEmoji = "🟠";
                                severityText = "Grave";
                                severityColor = Color.parseColor("#FF0000");
                                break;
                            case 2:
                                severityEmoji = "🟡";
                                severityText = "Moderado";
                                severityColor = Color.parseColor("#FFA500");
                                break;
                            case 1:
                                severityEmoji = "🟢";
                                severityText = "Leve";
                                severityColor = Color.parseColor("#FFD700");
                                break;
                            default:
                                severityEmoji = "⚪";
                                severityText = "Desconocido";
                                severityColor = Color.parseColor("#CCCCCC");
                        }
                        
                        severityInfoText.setText(severityEmoji + " Gravedad: " + severityText + " (" + severity + "/4)");
                        severityInfoText.setTextColor(severityColor);
                    }
                    
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Configurar el botón de seleccionar imagen
        selectImageButton.setOnClickListener(v -> {
            // Simular selección de imagen (en una app real se abriría la galería)
            imageSelectedText.setText("✅ Imagen seleccionada: foto_evidencia.jpg");
            imageSelectedText.setTextColor(Color.parseColor("#4CAF50")); // Verde
            Toast.makeText(this, "Función de galería no implementada en la demo", Toast.LENGTH_SHORT).show();
        });
        
        builder.setPositiveButton("Enviar Reporte", null); // Lo configuraremos después
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Configurar el botón "Enviar Reporte" después de mostrar el diálogo
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String address = addressInput.getText().toString().trim();
            String description = incidentDescriptionInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();
            String category = (String) categorySpinner.getSelectedItem();
            String subtype = (String) subtypeSpinner.getSelectedItem();
            
            // Validar campos obligatorios
            if (address.isEmpty()) {
                addressInput.setError("Por favor, ingresa la dirección");
                return;
            }
            
            if (description.isEmpty()) {
                incidentDescriptionInput.setError("Por favor, describe el incidente");
                return;
            }

            if (time.isEmpty()) {
                timeInput.setError("Por favor, indica cuándo ocurrió");
                return;
            }
            
            // Obtener email del usuario
            String userEmail = UserSession.getCurrentUserMail();
            if (userEmail == null) {
                Toast.makeText(MainActivity.this, "Error: No hay usuario en sesión", Toast.LENGTH_SHORT).show();
                return;
            }

            // **CORRECCIÓN BACKEND**: Obtener código backend y severidad del subtipo seleccionado
            Map<String, Object[]> subtypeData = subtypesByCategoryWithData.get(category);
            Object[] data = subtypeData.get(subtype);
            String backendCode = (String) data[0];  // Código que espera el backend
            int severity = (Integer) data[1];       // Severidad
            
            System.out.println("🎯 ENVIANDO AL BACKEND:");
            System.out.println("   ├─ Subtipo UI: '" + subtype + "'");
            System.out.println("   ├─ Código Backend: '" + backendCode + "'");
            System.out.println("   └─ Severidad: " + severity);
            
            // Deshabilitar botón para evitar múltiples envíos
            positiveButton.setEnabled(false);
            positiveButton.setText("Enviando...");

            // Geocodificar la dirección y enviar al backend
            new Thread(() -> {
                try {
                    GeoPoint location = getGeoPointFromAddress(address);
                    if (location == null) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Error: No se pudo encontrar la dirección",
                                    Toast.LENGTH_SHORT).show();
                            positiveButton.setEnabled(true);
                            positiveButton.setText("Enviar Reporte");
                        });
                        return;
                    }

                    // **CORRECCIÓN BACKEND**: Usar el código backend correcto
                    CrimeCreateRequest request = new CrimeCreateRequest(
                            backendCode,                // category = código backend (ej: "ROBO_CON_VIOLENCIA")
                            description,                // description 
                            address,                    // address
                            String.valueOf(location.getLatitude()),     // latitude
                            String.valueOf(location.getLongitude()),    // longitude
                            userEmail,                  // reporter
                            time                        // time
                    );

                    // Enviar al backend
                    ApiClient.getService().crearCrimen(request).enqueue(new Callback<CrimeDto>() {
                        @Override
                        public void onResponse(Call<CrimeDto> call, Response<CrimeDto> response) {
                            if (response.isSuccessful()) {
                                android.util.Log.d("MainActivity", "✅ Reporte creado exitosamente");
                                runOnUiThread(() -> {
                                    dialog.dismiss();
                                    showReportSuccessDialog(category, subtype, severity);
                                    Toast.makeText(MainActivity.this,
                                            "✅ Reporte enviado exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                    android.util.Log.d("MainActivity", "🔄 Recargando crímenes...");
                                    loadCrimesFromBackend();
                                });
                            } else {
                                android.util.Log.e("MainActivity", "❌ Error al crear reporte: código " + response.code());
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Error al enviar reporte (código: " + response.code() + ")",
                                            Toast.LENGTH_SHORT).show();
                                    positiveButton.setEnabled(true);
                                    positiveButton.setText("Enviar Reporte");
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<CrimeDto> call, Throwable t) {
                            android.util.Log.e("MainActivity", "❌ Error de conexión: " + t.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Error de conexión: " + t.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                positiveButton.setEnabled(true);
                                positiveButton.setText("Enviar Reporte");
                            });
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Error al procesar la dirección",
                                Toast.LENGTH_SHORT).show();
                        positiveButton.setEnabled(true);
                        positiveButton.setText("Enviar Reporte");
                    });
                }
            }).start();
        });
    }
    
    private void showReportSuccessDialog(String category, String subtype, int severity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Reporte Enviado");
        
        String severityEmoji;
        String severityText;
        switch (severity) {
            case 4:
                severityEmoji = "🔴";
                severityText = "Muy Grave";
                break;
            case 3:
                severityEmoji = "🟠";
                severityText = "Grave";
                break;
            case 2:
                severityEmoji = "🟡";
                severityText = "Moderado";
                break;
            case 1:
                severityEmoji = "🟢";
                severityText = "Leve";
                break;
            default:
                severityEmoji = "⚪";
                severityText = "Desconocido";
        }
        
        builder.setMessage("Gracias por tu reporte. La información ha sido registrada y contribuirá a mejorar la seguridad en la zona.\n\n" +
                          "📋 Resumen del reporte:\n" +
                          "📂 Categoría: " + category + "\n" +
                          "🚨 Tipo: " + subtype + "\n" +
                          severityEmoji + " Gravedad: " + severityText + " (" + severity + "/4)\n\n" +
                          "📊 Tu reporte ayudará a:\n" +
                          "• Identificar zonas de riesgo\n" +
                          "• Alertar a otros usuarios\n" +
                          "• Mejorar las rutas seguras\n\n" +
                          "🔒 Toda la información es tratada de forma confidencial.\n\n" +
                          "💡 Tip: Puedes ver el estado de tus reportes en 'Mis Reportes'");

        builder.setPositiveButton("Ver Mis Reportes", (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, MyCrimesActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.setIcon(R.drawable.ic_alert_warning);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    
    // ★★★ MÉTODO ESPECÍFICO PARA WAYPOINTS DE PEATONES ★★★
    
    private void addPedestrianSpecificWaypoints(GeoPoint start, GeoPoint end, List<GeoPoint> waypoints) {
        // Limitar a máximo 1 waypoint adicional para peatones
        if (waypoints.size() >= 2) {
            return; // Ya hay suficientes waypoints
        }
        
        // Buscar crímenes en vía pública específicamente
        List<CrimeAlert> streetCrimes = new ArrayList<>();
        for (CrimeAlert crime : crimeAlerts) {
            if ("Crimen en vía pública".equals(crime.crimeType) && crime.location != null) {
                streetCrimes.add(crime);
            }
        }
        
        if (streetCrimes.isEmpty()) {
            return; // No hay crímenes en vía pública que evitar
        }
        
        // Buscar el crimen más cercano a la ruta directa
        double startLat = start.getLatitude();
        double startLon = start.getLongitude();
        double endLat = end.getLatitude();
        double endLon = end.getLongitude();
        
        CrimeAlert closestCrime = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Verificar múltiples puntos a lo largo de la ruta
        for (int i = 1; i <= 4; i++) {
            double ratio = (double) i / 5;
            double checkLat = startLat + (endLat - startLat) * ratio;
            double checkLon = startLon + (endLon - startLon) * ratio;
            GeoPoint checkPoint = new GeoPoint(checkLat, checkLon);

            for (CrimeAlert crime : streetCrimes) {
                double distance = calculateDistance(checkPoint, crime.location);
                if (distance < closestDistance && distance <= 300) { // Radio de 300m para peatones
                    closestDistance = distance;
                    closestCrime = crime;
                }
            }
        }
        
        // Si encontramos un crimen cercano, crear un waypoint de evasión
        if (closestCrime != null) {
            // Crear un waypoint que desvíe la ruta
            double avoidanceLat = closestCrime.location.getLatitude() + 0.002; // ~200m de desvío
            double avoidanceLon = closestCrime.location.getLongitude() + 0.002;

            // Verificar que el punto de evasión no esté cerca de otros peligros
            GeoPoint avoidancePoint = new GeoPoint(avoidanceLat, avoidanceLon);
            if (!isPointNearDanger(avoidancePoint)) {
                waypoints.add(avoidancePoint);
                System.out.println("Waypoint de evasión para peatón agregado: " + avoidanceLat + "," + avoidanceLon);
            } else {
                // Intentar en dirección opuesta
                avoidanceLat = closestCrime.location.getLatitude() - 0.002;
                avoidanceLon = closestCrime.location.getLongitude() - 0.002;
                avoidancePoint = new GeoPoint(avoidanceLat, avoidanceLon);
                if (!isPointNearDanger(avoidancePoint)) {
                    waypoints.add(avoidancePoint);
                    System.out.println("Waypoint de evasión alternativo para peatón agregado: " + avoidanceLat + "," + avoidanceLon);
                }
            }
        }
    }

    // ★★★ MÉTODO ESPECÍFICO PARA WAYPOINTS DE VEHÍCULOS ★★★

    private void addVehicleSpecificWaypoints(GeoPoint start, GeoPoint end, List<GeoPoint> waypoints) {
        // Limitar a máximo 1 waypoint adicional para vehículos
        if (waypoints.size() >= 2) {
            return; // Ya hay suficientes waypoints
        }

        // Buscar robos de vehículos específicamente
        List<CrimeAlert> vehicleThefts = new ArrayList<>();
        for (CrimeAlert crime : crimeAlerts) {
            if ("Robo de vehículos".equals(crime.crimeType) && crime.location != null) {
                vehicleThefts.add(crime);
            }
        }

        if (vehicleThefts.isEmpty()) {
            return; // No hay robos de vehículos que evitar
        }

        // Buscar solo el robo de vehículo más cercano a la ruta directa
        double startLat = start.getLatitude();
        double startLon = start.getLongitude();
        double endLat = end.getLatitude();
        double endLon = end.getLongitude();

        CrimeAlert closestTheft = null;
        double closestDistance = Double.MAX_VALUE;

        // Encontrar el robo más cercano al punto medio de la ruta
        double midLat = startLat + 0.5 * (endLat - startLat);
        double midLon = startLon + 0.5 * (endLon - startLon);
        GeoPoint midPoint = new GeoPoint(midLat, midLon);

        for (CrimeAlert theft : vehicleThefts) {
            double distance = calculateDistance(midPoint, theft.location);
            if (distance < closestDistance && distance <= 500) { // Solo si está dentro de 500m
                closestDistance = distance;
                closestTheft = theft;
            }
        }

        // Si encontramos un robo cercano, crear un waypoint de evasión
        if (closestTheft != null) {
            // Crear un waypoint que desvíe la ruta de manera más conservadora
            double avoidanceLat = closestTheft.location.getLatitude() + 0.003; // ~300m de desvío
            double avoidanceLon = closestTheft.location.getLongitude() + 0.003;

            // Verificar que el punto de evasión no esté cerca de otros peligros
            GeoPoint avoidancePoint = new GeoPoint(avoidanceLat, avoidanceLon);
            if (!isPointNearDanger(avoidancePoint)) {
                waypoints.add(avoidancePoint);
                System.out.println("Waypoint de evasión de robo de vehículo agregado: " + avoidanceLat + "," + avoidanceLon);
            }
        }
    }

    /**
     * **NUEVO**: Configura datos hardcodeados de crímenes para fallback
     * Basado en la implementación funcional anterior
     */
    private void setupHardcodedCrimeAlerts() {
        // Limpiar alertas actuales
        crimeAlerts.clear();
        
        System.out.println("🎯 Configurando datos hardcodeados de crímenes como fallback...");
        System.out.println("📊 Estado de filtros al configurar datos:");
        System.out.println("   ├─ showStreetCrime: " + showStreetCrime);
        System.out.println("   └─ showVehicleCrime: " + showVehicleCrime);
        
        // ========== DELITOS CONTRA LAS PERSONAS ==========
        // Alertas de crímenes contra transeúntes en la vía pública
        
        // Alerta 1: Av. Corrientes 300 - Robo con arma (Grave)
        CrimeAlert alert1 = new CrimeAlert(
                1001, // ID hardcodeado
                "Robo a mano armada",
                "Se reportó un robo a mano armada en esta zona. El incidente ocurrió en horario nocturno cuando la víctima caminaba sola.",
                "Av. Corrientes 300, Buenos Aires, Argentina",
                "Hace 2 días",
                "Crimen en vía pública", // crimeType correcto
                "Delitos contra las personas",
                "Robo/Arrebato",
                3, // Grave
                "usuario.ejemplo@mail.com",
                5, // 5 verificaciones
                "CONFIRMADO"
        );
        alert1.location = new GeoPoint(-34.6037, -58.3816); // Coordenadas directas para evitar geocoding
        crimeAlerts.add(alert1);

        // Alerta 2: Av. Corrientes 600 - Robo de pertenencias (Moderado)
        CrimeAlert alert2 = new CrimeAlert(
                1002,
                "Robo de pertenencias",
                "Robo de celular y billetera reportado por transeúntes. Los delincuentes escaparon en motocicleta.",
                "Av. Corrientes 600, Buenos Aires, Argentina",
                "Hace 1 semana",
                "Crimen en vía pública", // crimeType correcto
                "Delitos contra las personas",
                "Robo/Arrebato",
                2, // Moderado
                "testeo@gmail.com",
                3,
                "CONFIRMADO"
        );
        alert2.location = new GeoPoint(-34.6035, -58.3820);
        crimeAlerts.add(alert2);

        // Alerta 3: Florida 300 - Arrebato (Moderado)
        CrimeAlert alert3 = new CrimeAlert(
                1003,
                "Arrebato de cartera",
                "Arrebato de cartera en la zona peatonal durante el horario comercial. La víctima reportó que fueron dos personas en bicicleta.",
                "Florida 300, Buenos Aires, Argentina",
                "Hace 3 días",
                "Crimen en vía pública", // crimeType correcto
                "Delitos contra las personas",
                "Robo/Arrebato",
                2, // Moderado
                "seguridad@ciudad.gov.ar",
                8,
                "CONFIRMADO"
        );
        alert3.location = new GeoPoint(-34.6010, -58.3750);
        crimeAlerts.add(alert3);

        // Alerta 4: Hurto leve (Leve)
        CrimeAlert alert4 = new CrimeAlert(
                1004,
                "Hurto por distracción",
                "Hurto de billetera mediante distracción en zona comercial. Los delincuentes operaban en grupo fingiendo ser compradores.",
                "Lavalle 600, Buenos Aires, Argentina",
                "Hace 5 días",
                "Crimen en vía pública", // crimeType correcto
                "Delitos contra las personas",
                "Hurto",
                1, // Leve
                "comerciante@zona.com",
                2,
                "PENDIENTE"
        );
        alert4.location = new GeoPoint(-34.6005, -58.3755);
        crimeAlerts.add(alert4);

        // ========== DELITOS CONTRA LA PROPIEDAD (VEHÍCULOS) ==========

        // Robo de vehículo 1: Muy grave
        CrimeAlert vehicleAlert1 = new CrimeAlert(
                1005,
                "Robo de automóvil",
                "Robo de vehículo Toyota Corolla blanco en estacionamiento. Los delincuentes forzaron la cerradura y se llevaron el auto en menos de 3 minutos.",
                "Av. Corrientes 450, Buenos Aires, Argentina",
                "Hace 6 horas",
                "Robo de vehículos", // crimeType correcto para filtros
                "Delitos contra la propiedad",
                "Robo de vehículo estacionado",
                4, // Muy grave
                "propietario@vehiculo.com",
                12,
                "CONFIRMADO"
        );
        vehicleAlert1.location = new GeoPoint(-34.6038, -58.3818);
        crimeAlerts.add(vehicleAlert1);

        // Robo de vehículo 2: Grave
        CrimeAlert vehicleAlert2 = new CrimeAlert(
                1006,
                "Robo de motocicleta",
                "Robo de motocicleta Honda en la vía pública. Dos personas intimidaron al conductor y se llevaron el vehículo.",
                "Av. Santa Fe 700, Buenos Aires, Argentina",
                "Hace 1 día",
                "Robo de vehículos", // crimeType correcto para filtros
                "Delitos contra la propiedad",
                "Robo de vehículo en movimiento",
                3, // Grave
                "motorista@delivery.com",
                7,
                "CONFIRMADO"
        );
        vehicleAlert2.location = new GeoPoint(-34.5952, -58.3785);
        crimeAlerts.add(vehicleAlert2);

        // Robo de bicicleta (Moderado)
        CrimeAlert bikeAlert = new CrimeAlert(
                1007,
                "Robo de bicicleta",
                "Robo de bicicleta de alta gama que estaba asegurada con cadena. Los delincuentes cortaron la cadena con herramientas.",
                "Plaza San Martín, Buenos Aires, Argentina",
                "Hace 3 días",
                "Robo de vehículos", // crimeType correcto para filtros
                "Delitos contra la propiedad",
                "Robo de bicicleta",
                2, // Moderado
                "ciclista@urbano.com",
                4,
                "CONFIRMADO"
        );
        bikeAlert.location = new GeoPoint(-34.5975, -58.3756);
        crimeAlerts.add(bikeAlert);

        System.out.println("✅ Configurados " + crimeAlerts.size() + " crímenes hardcodeados con diferentes severidades");
        
        // **DEBUGGING**: Imprimir todos los crímenes configurados
        for (int i = 0; i < crimeAlerts.size(); i++) {
            CrimeAlert alert = crimeAlerts.get(i);
            System.out.println("   " + (i+1) + ". " + alert.title + " | Tipo: '" + alert.crimeType + "' | Categoría: '" + alert.category + "'");
        }
    }

    // ★★★ MÉTODOS PARA FILTROS DE CRIMEN ★★★

    private void toggleStreetCrimeFilter() {
        showStreetCrime = !showStreetCrime;
        updateCrimeFilterButtons();
        refreshCrimeDisplay();

        String status = showStreetCrime ? "mostrar" : "ocultar";
        Toast.makeText(this, "Filtro: " + status + " crímenes en vía pública", Toast.LENGTH_SHORT).show();
    }

    private void toggleVehicleCrimeFilter() {
        showVehicleCrime = !showVehicleCrime;
        updateCrimeFilterButtons();
        refreshCrimeDisplay();
        
        String status = showVehicleCrime ? "mostrar" : "ocultar";
        Toast.makeText(this, "Filtro: " + status + " robos de vehículos", Toast.LENGTH_SHORT).show();
    }
    
    private void updateCrimeFilterButtons() {
        // Actualizar el color de los botones según el estado activo
        streetCrimeFilterButton.setBackgroundTintList(ColorStateList.valueOf(
            showStreetCrime ? Color.parseColor("#F44336") : Color.parseColor("#CCCCCC")));
        
        vehicleCrimeFilterButton.setBackgroundTintList(ColorStateList.valueOf(
            showVehicleCrime ? Color.parseColor("#9C27B0") : Color.parseColor("#CCCCCC")));
    }
    
    /**
     * **MEJORADO**: Refresca la visualización de crímenes usando la nueva lógica
     */
    private void refreshCrimeDisplay() {
        System.out.println("🔄 Refrescando display de crímenes - Calles: " + showStreetCrime + ", Vehículos: " + showVehicleCrime);
        
        // **MEJORADO**: Limpiar marcadores de crímenes existentes usando la lista de seguimiento
        map.getOverlays().removeAll(crimeAlertMarkers);
        crimeAlertMarkers.clear();
        
        // **MEJORADO**: Limpiar zonas de peligro existentes usando la lista de seguimiento
        hideDangerZones();
        
        // PRIMERO: Agregar zonas de peligro (círculos) - van al fondo
        if (showStreetCrime || showVehicleCrime) {
            createFilteredDangerZones();
        }
        
        // SEGUNDO: Agregar marcadores encima - quedan clickeables
        addFilteredCrimeAlertsToMap();

        map.invalidate(); // Refrescar el mapa
        
        // **DEBUG**: Mostrar estadísticas
        int visibleCrimes = 0;
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location != null && shouldShowCrime(alert)) {
                visibleCrimes++;
            }
        }
        System.out.println("📊 Crímenes visibles después del filtro: " + visibleCrimes + "/" + crimeAlerts.size());
    }
    
    /**
     * **MEJORADO**: Agrega marcadores filtrados usando la nueva lógica de marcadores mejorados
     */
    private void addFilteredCrimeAlertsToMap() {
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location == null) continue;
            
            boolean shouldShow = shouldShowCrime(alert);
            
            if (shouldShow) {
                // **CORRECCIÓN CRÍTICA**: Determinar isActive basándose SOLO en el estado del crimen
                boolean isActive = "CONFIRMADO".equals(alert.status);
                
                // **USAR EL NUEVO MÉTODO MEJORADO** con el estado correcto
                Marker marker = createCrimeMarker(alert, isActive);
                map.getOverlays().add(marker);
                crimeAlertMarkers.add(marker);
                
                // Agregar animación de rebote
                startCrimeAlertAnimation(marker);
            }
        }
    }

    /**
     * **NUEVO**: Determina si un crimen involucra vehículos basándose en la descripción y código
     */
    private boolean isVehicleRelatedCrime(String description, String crimeCode) {
        if (description == null) return false;
        
        String descLower = description.toLowerCase();
        
        // Palabras clave que indican crimen de vehículos
        boolean hasVehicleKeywords = descLower.contains("vehículo") ||
                                   descLower.contains("vehiculo") ||
                                   descLower.contains("auto") ||
                                   descLower.contains("carro") ||
                                   descLower.contains("coche") ||
                                   descLower.contains("moto") ||
                                   descLower.contains("motocicleta") ||
                                   descLower.contains("bicicleta") ||
                                   descLower.contains("scooter") ||
                                   descLower.contains("camioneta") ||
                                   descLower.contains("taxi") ||
                                   descLower.contains("uber") ||
                                   descLower.contains("estacionamiento") ||
                                   descLower.contains("garage");
        
        System.out.println("🚗 Análisis de vehículo para '" + crimeCode + "':");
        System.out.println("   ├─ Descripción: '" + description + "'");
        System.out.println("   └─ Es vehículo: " + hasVehicleKeywords);
        
        return hasVehicleKeywords;
    }

    /**
     * **CORREGIDO**: Método para determinar si un crimen debe mostrarse según los filtros actuales
     * Usa la MISMA lógica que la implementación funcional original
     */
    private boolean shouldShowCrime(CrimeAlert alert) {
        // **DEBUG LOGS**
        System.out.println("🔍 Evaluando crimen: '" + alert.title + "'");
        System.out.println("   ├─ crimeType: '" + alert.crimeType + "'");
        System.out.println("   ├─ category: '" + alert.category + "'");
        System.out.println("   ├─ showVehicleCrime: " + showVehicleCrime);
        System.out.println("   └─ showStreetCrime: " + showStreetCrime);
        
        // **LÓGICA ORIGINAL RESTAURADA**: Usar crimeType exactamente como en "trabajo final bien hecho"
        boolean result = false;
        if ("Robo de vehículos".equals(alert.crimeType) && showVehicleCrime) {
            result = true;
            System.out.println("   ✅ MOSTRAR (Robo de vehículos)");
        } else if ("Crimen en vía pública".equals(alert.crimeType) && showStreetCrime) {
            result = true;
            System.out.println("   ✅ MOSTRAR (Crimen en vía pública)");
        } else {
            System.out.println("   ❌ OCULTAR (No coincide con filtros)");
        }
        
        return result;
    }
    
    /**
     * **MEJORADO**: Crea zonas de peligro filtradas usando la nueva lógica de severidad
     */
    private void createFilteredDangerZones() {
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location == null) continue;
            
            boolean shouldShow = shouldShowCrime(alert);
            
            if (shouldShow) {
                // **USAR LA FUNCIÓN MEJORADA** que considera la gravedad y crea zonas diferenciadas
                createDangerZone(alert.location, alert.severity);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    // ================================
    // SISTEMA DE PUNTOS Y LOGROS
    // ================================
    
    /**
     * Verifica recompensas solo después de una verificación exitosa
     * Evita llamadas innecesarias al backend
     */
    private void checkUserRewardsAfterVerification() {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) return;

        UserMailRequest request = new UserMailRequest(userEmail);
        ApiClient.getService().getUsuario(request).enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    UserResponse user = response.body().get(0);
                    
                    // Solo mostrar notificación si los puntos cambiaron
                    if (user.points > lastKnownPoints) {
                        int pointsGained = user.points - lastKnownPoints;
                        showPointsNotification(pointsGained);
                        lastKnownPoints = user.points;
                    }
                    
                    // Verificar logros desbloqueados
                    checkForNewAchievements(user);
                }
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                // Silenciar errores de red para no molestar al usuario
            }
        });
    }

    /**
     * Muestra notificación de puntos ganados
     */
    private void showPointsNotification(int pointsGained) {
        runOnUiThread(() -> {
            Toast.makeText(this, 
                "🎉 +" + pointsGained + " puntos ganados!", 
                Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Verifica logros basándose en la estructura del backend:
     * CONFIRMATION: 3, 10, 20 reportes confirmados
     * VALIDATION: 15, 50, 100 validaciones
     */
    private void checkForNewAchievements(UserResponse user) {
        if (user.achievements == null) return;
        
        // Verificar si hay nuevos logros comparando con el último estado conocido
        // Por simplicidad, mostrar notificación de los últimos logros obtenidos
        for (Logro achievement : user.achievements) {
            // Solo mostrar los más recientes basados en el progreso actual
            if (achievement.category.equals("CONFIRMATION")) {
                if (user.confirmedReports == achievement.requirements) {
                    showAchievementUnlocked(achievement.name, "¡Tienes " + achievement.requirements + " reportes confirmados!");
                }
            } else if (achievement.category.equals("VALIDATION")) {
                if (user.validations == achievement.requirements) {
                    showAchievementUnlocked(achievement.name, "¡Realizaste " + achievement.requirements + " verificaciones!");
                }
            }
        }
    }

    /**
     * Muestra notificación de logro desbloqueado
     */
    private void showAchievementUnlocked(String achievementName, String description) {
        runOnUiThread(() -> {
            Toast.makeText(this, 
                "🏆 LOGRO DESBLOQUEADO: " + achievementName + "\n" + description, 
                Toast.LENGTH_LONG).show();
        });
    }
}

