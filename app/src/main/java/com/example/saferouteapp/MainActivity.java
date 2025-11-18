package com.example.saferouteapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
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
import org.osmdroid.views.overlay.infowindow.InfoWindow;
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

public class MainActivity extends AppCompatActivity {
    private MapView map;
    private EditText originEditText, destinationEditText;
    private Button routeButton, vehicleRouteButton;
    private ImageButton backButton;
    private Marker originMarker, destinationMarker;
    private FloatingActionButton menuButton, zoomInButton, zoomOutButton,
            streetCrimeFilterButton, vehicleCrimeFilterButton;
    private Button reportCrimeButton;
    
    // Sistema de puntos
    private TextView pointsTextView;
    private Button exchangePointsButton;
    private int userPoints = 0;
    private static final String PREFS_NAME = "SafeRoutePrefs";
    private static final String KEY_POINTS = "user_points";

    private final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibHVjYXNhZzA1IiwiYSI6ImNtZ3poaTdxMDAwOGcyaXBxYWRvYzJkanIifQ.2tJ3eYfxB8W5NbeQTKNHwA";

    private LinearLayout routeInfoLayout, searchLayout;
    private LinearLayout safeRouteOption, fastRouteOption;
    private LinearLayout exportButtonsLayout;
    private TextView safeRouteInfo, fastRouteInfo;
    private ImageView fastRouteRadio;
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
        final String title;
        final String description;
        final String address; // Dirección que será geocodificada
        final String timeAgo;
        final String crimeType; // Tipo de crimen (mantener para compatibilidad)
        final String category; // "Delitos contra las personas" o "Delitos contra la propiedad"
        final String subType; // Subtipo específico del crimen
        final int severity; // Gravedad: 1 (leve) a 4 (grave)
        final boolean isActive; // true = activo (afecta rutas), false = inactivo (solo visual)
        GeoPoint location; // Se establecerá después de la geocodificación

        CrimeAlert(String title, String description, String address, String timeAgo, String crimeType, 
                   String category, String subType, int severity, boolean isActive) {
            this.title = title;
            this.description = description;
            this.address = address;
            this.timeAgo = timeAgo;
            this.crimeType = crimeType;
            this.category = category;
            this.subType = subType;
            this.severity = severity;
            this.isActive = isActive;
            this.location = null; // Se establecerá más tarde
        }
        
        // Constructor de compatibilidad (por defecto activo)
        CrimeAlert(String title, String description, String address, String timeAgo, String crimeType, 
                   String category, String subType, int severity) {
            this(title, description, address, timeAgo, crimeType, category, subType, severity, true);
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
    private final List<CrimeAlert> inactiveCrimeAlerts = new ArrayList<>();
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
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("OSM", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);

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
                return false;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(0, mapEventsOverlay); // Agregar como primer overlay

        IMapController mapController = map.getController();
        mapController.setZoom(16.0); // Zoom para ver bien la ubicación del usuario
        // Centro inicial en la ubicación del usuario
        mapController.setCenter(currentUserLocation);

        originEditText = findViewById(R.id.origin_text);
        destinationEditText = findViewById(R.id.destination_text);
        
        // Configurar hint para mostrar la ubicación actual
        originEditText.setHint("📍 Ubicación actual (Av. Santa Fe 995)");
        
        routeButton = findViewById(R.id.route_button);
        vehicleRouteButton = findViewById(R.id.vehicle_route_button);
        routeInfoLayout = findViewById(R.id.route_info_layout);
        searchLayout = findViewById(R.id.search_layout);
        safeRouteOption = findViewById(R.id.safe_route_option);
        fastRouteOption = findViewById(R.id.fast_route_option);
        safeRouteInfo = findViewById(R.id.safe_route_info);
        fastRouteInfo = findViewById(R.id.fast_route_info);
        fastRouteRadio = findViewById(R.id.fast_route_radio);
        menuButton = findViewById(R.id.menu_button);
        reportCrimeButton = findViewById(R.id.report_crime_button);
        backButton = findViewById(R.id.back_button);
        zoomInButton = findViewById(R.id.zoom_in_button);
        zoomOutButton = findViewById(R.id.zoom_out_button);
        streetCrimeFilterButton = findViewById(R.id.street_crime_filter_button);
        vehicleCrimeFilterButton = findViewById(R.id.vehicle_crime_filter_button);
        
        // Inicializar sistema de puntos
        pointsTextView = findViewById(R.id.points_text_view);
        exchangePointsButton = findViewById(R.id.exchange_points_button);
        loadUserPoints();
        updatePointsDisplay();
        
        // Inicializar layout y botones de exportar
        exportButtonsLayout = findViewById(R.id.export_buttons_layout);
        exportUberButton = findViewById(R.id.export_uber_button);
        exportPedidosYaButton = findViewById(R.id.export_pedidosya_button);


        routeButton.setOnClickListener(v -> {
            String originAddress = originEditText.getText().toString().trim();
            String destinationAddress = destinationEditText.getText().toString().trim();

            // Si el origen está vacío, usar la ubicación actual
            if (originAddress.isEmpty()) {
                originAddress = "Av. Santa Fe 995, Buenos Aires, Argentina";
            }
            
            if (destinationAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa un destino", Toast.LENGTH_SHORT).show();
                return;
            }
            vehicleMode = false;
            hideKeyboard();
            calculateBothRoutes(originAddress, destinationAddress);
        });
        
        vehicleRouteButton.setOnClickListener(v -> {
            String originAddress = originEditText.getText().toString().trim();
            String destinationAddress = destinationEditText.getText().toString().trim();

            // Si el origen está vacío, usar la ubicación actual
            if (originAddress.isEmpty()) {
                originAddress = "Av. Santa Fe 995, Buenos Aires, Argentina";
            }
            
            if (destinationAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa un destino", Toast.LENGTH_SHORT).show();
                return;
            }
            vehicleMode = true;
            hideKeyboard();
            calculateBothRoutes(originAddress, destinationAddress);
        });

        // Configurar listeners para las opciones de ruta
        safeRouteOption.setOnClickListener(v -> selectRouteType(true));
        fastRouteOption.setOnClickListener(v -> selectRouteType(false));
        
        // Configurar listeners para botones de exportar
        exportUberButton.setOnClickListener(v -> exportToUber());
        exportPedidosYaButton.setOnClickListener(v -> exportToPedidosYa());

        menuButton.setOnClickListener(v -> Toast.makeText(this, "Botón de Menú presionado", Toast.LENGTH_SHORT).show());
        reportCrimeButton.setOnClickListener(v -> showReportCrimeDialog());
        
        exchangePointsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RewardsActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> clearRoute());

        zoomInButton.setOnClickListener(v -> map.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> map.getController().zoomOut());
        
        streetCrimeFilterButton.setOnClickListener(v -> toggleStreetCrimeFilter());
        vehicleCrimeFilterButton.setOnClickListener(v -> toggleVehicleCrimeFilter());

        // Agregar marcador de ubicación actual del usuario
        addUserLocationMarker();
        
        setupSafePoints();
        addSafePointsToMap();
        setupCrimeAlerts();
        addCrimeAlertsToMap();
        
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
    }

    private void addUserLocationMarker() {
        // Crear marcador para la ubicación actual del usuario
        userLocationMarker = new Marker(map);
        userLocationMarker.setPosition(currentUserLocation);
        userLocationMarker.setTitle("Tu ubicación");
        userLocationMarker.setSnippet("Av. Santa Fe 995, Buenos Aires");
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        // Usar el icono personalizado de ubicación
        Drawable userLocationIcon = ContextCompat.getDrawable(this, R.drawable.ic_my_location);
        userLocationMarker.setIcon(userLocationIcon);
        
        // Hacer que el marcador sea más visible
        userLocationMarker.setAlpha(1.0f);
        
        // Agregar el marcador al mapa
        map.getOverlays().add(userLocationMarker);
        
        // Animar el marcador (efecto de pulso)
        startLocationMarkerAnimation();
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
        Drawable policeIcon = ContextCompat.getDrawable(this, R.drawable.ic_police_station);
        Drawable hospitalIcon = ContextCompat.getDrawable(this, R.drawable.ic_hospital);

        for (SafePoint point : safePoints) {
            Marker marker = new Marker(map);
            marker.setPosition(point.location);
            marker.setTitle(point.name);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            if ("police".equals(point.type)) {
                marker.setIcon(policeIcon);
            } else {
                marker.setIcon(hospitalIcon);
            }
            map.getOverlays().add(marker);
            safePointMarkers.add(marker);
        }
        map.invalidate();
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
        map.getOverlays().removeAll(crimeAlertMarkers);
        crimeAlertMarkers.clear();
        
        // Volver a agregar los marcadores de puntos seguros y alertas de crimen
        addSafePointsToMap();
        addCrimeAlertsToMap(); // Esto volverá a geocodificar y agregar las alertas
        
        map.invalidate();
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

    private void selectRouteType(boolean selectSafe) {
        safeRouteSelected = selectSafe;
        
        // Actualizar UI
        if (selectSafe) {
            safeRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            fastRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            fastRouteRadio.setImageResource(android.R.drawable.radiobutton_off_background);
        } else {
            safeRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            fastRouteOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
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

        // Dibujar ruta rápida con color según el modo
        if (!fastRoutes.isEmpty()) {
            RouteInfo fastRoute = fastRoutes.get(0);
            int fastColor = vehicleMode ? Color.parseColor("#FF6600") : Color.RED; // Naranja para vehículo, rojo para peatón
            drawSingleRoute(fastRoute.points, fastColor, 6.0f);
        }

        // Dibujar ruta segura con color según el modo
        if (!safeRoutes.isEmpty()) {
            RouteInfo safeRoute = safeRoutes.get(0);
            int safeColor = vehicleMode ? Color.parseColor("#00AA00") : Color.BLUE; // Verde para vehículo, azul para peatón
            drawSingleRoute(safeRoute.points, safeColor, 8.0f);
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
        
        // Primero, verificar la ruta directa
        List<RouteInfo> directRoute = getRoutes(start, end);
        if (directRoute.isEmpty()) {
            return directRoute;
        }
        
        // Analizar si la ruta directa pasa por zonas peligrosas
        List<CrimeAlert> dangerousPoints = findDangersInRoute(directRoute.get(0));
        
        if (dangerousPoints.isEmpty()) {
            System.out.println("✅ Ruta directa es segura, no hay peligros en el camino");
            return directRoute;
        }
        
        System.out.println("⚠️ Ruta directa pasa por " + dangerousPoints.size() + " zona(s) de peligro");
        for (CrimeAlert danger : dangerousPoints) {
            System.out.println("  - " + danger.subType + " (Nivel " + danger.severity + ")");
        }
        
        // Generar waypoints para evitar las zonas peligrosas
        List<GeoPoint> avoidanceWaypoints = generateAvoidanceWaypoints(start, end, dangerousPoints);
        
        if (avoidanceWaypoints.isEmpty()) {
            System.out.println("❌ No se pueden generar waypoints de evasión, usando ruta directa");
            return directRoute;
        }
        
        System.out.println("🔄 Generando ruta alternativa con " + avoidanceWaypoints.size() + " waypoint(s) de evasión");
        
        // Construir ruta con waypoints
        return buildRouteWithWaypoints(start, end, avoidanceWaypoints);
    }
    
    private List<CrimeAlert> findDangersInRoute(RouteInfo route) {
        List<CrimeAlert> dangers = new ArrayList<>();
        
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location == null) continue;
            
            // Radio de detección según gravedad
            double dangerRadius;
            switch (crime.severity) {
                case 1: dangerRadius = 40; break;
                case 2: dangerRadius = 80; break;
                case 3: dangerRadius = 180; break;
                case 4: dangerRadius = 250; break;
                default: dangerRadius = 100; break;
            }
            
            // Verificar si algún punto de la ruta pasa por esta zona peligrosa
            for (GeoPoint point : route.points) {
                double distance = calculateDistance(point, crime.location);
                if (distance <= dangerRadius) {
                    if (!dangers.contains(crime)) {
                        dangers.add(crime);
                    }
                    break;
                }
            }
        }
        
        return dangers;
    }
    
    private List<GeoPoint> generateAvoidanceWaypoints(GeoPoint start, GeoPoint end, List<CrimeAlert> dangers) {
        List<GeoPoint> waypoints = new ArrayList<>();
        
        // Calcular vector de dirección de la ruta
        double dx = end.getLongitude() - start.getLongitude();
        double dy = end.getLatitude() - start.getLatitude();
        double routeLength = Math.sqrt(dx * dx + dy * dy);
        
        if (routeLength == 0) return waypoints;
        
        // Vector perpendicular normalizado (para desviar a los lados)
        double perpX = -dy / routeLength;
        double perpY = dx / routeLength;
        
        for (CrimeAlert danger : dangers) {
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
            
            // Radio de peligro
            double dangerRadius;
            switch (danger.severity) {
                case 1: dangerRadius = 40; break;
                case 2: dangerRadius = 80; break;
                case 3: dangerRadius = 180; break;
                case 4: dangerRadius = 250; break;
                default: dangerRadius = 100; break;
            }
            
            // Si el punto más cercano está dentro del radio de peligro, crear waypoint de evasión
            if (distanceToDanger <= dangerRadius) {
                // Calcular cuánto desviar (radio + margen de seguridad)
                double deviationDistance = (dangerRadius - distanceToDanger + 50) / 111000.0; // +50m de margen, convertir a grados
                
                // Intentar desviar perpendicular a la ruta (ambos lados)
                GeoPoint waypoint1 = new GeoPoint(
                    closestLat + perpY * deviationDistance,
                    closestLon + perpX * deviationDistance
                );
                
                GeoPoint waypoint2 = new GeoPoint(
                    closestLat - perpY * deviationDistance,
                    closestLon - perpX * deviationDistance
                );
                
                // Elegir el waypoint que esté más lejos de TODOS los peligros
                double dist1 = getMinDistanceToDangers(waypoint1, crimeAlerts);
                double dist2 = getMinDistanceToDangers(waypoint2, crimeAlerts);
                
                GeoPoint chosenWaypoint = (dist1 > dist2) ? waypoint1 : waypoint2;
                
                waypoints.add(chosenWaypoint);
                System.out.println("  ✓ Waypoint de evasión: " + 
                    String.format("%.6f, %.6f", chosenWaypoint.getLatitude(), chosenWaypoint.getLongitude()) +
                    " (desviación: " + String.format("%.0f", deviationDistance * 111000) + "m)");
            }
        }
        
        // Limitar a máximo 5 waypoints
        if (waypoints.size() > 5) {
            waypoints = waypoints.subList(0, 5);
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
        
        // Riesgo por alertas de crimen individuales
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                double distance = calculateDistance(point, crime.location);
                
                // Radio de influencia ajustado según gravedad del crimen
                // Nivel 1: 40m + (1 * 53.33) = 93.33m ≈ 93m
                // Nivel 2: 40m + (2 * 53.33) = 146.67m ≈ 147m
                // Nivel 3: 40m + (3 * 53.33) = 200m
                // Nivel 4: 40m + (4 * 53.33) = 253.33m ≈ 253m, pero limitamos a 200m
                double influenceRadius = Math.min(40 + (crime.severity * 53.33), 200); // 40m para nivel 1, 200m para nivel 4
                
                // Riesgo decae exponencialmente con la distancia
                if (distance <= influenceRadius) {
                    // Factor base de riesgo según gravedad (nivel 4 = 4x más riesgo que nivel 1)
                    double severityWeight = crime.severity * 12.5; // 12.5 para nivel 1, 50.0 para nivel 4
                    
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

    private void setupCrimeAlerts() {
        // ========== DELITOS CONTRA LAS PERSONAS ==========
        // Alertas de crímenes contra transeúntes en la vía pública
        // Usando direcciones reales que serán geocodificadas por la API

        // Alerta 1: Av. Corrientes 300 - Robo con arma
        crimeAlerts.add(new CrimeAlert(
                "Robo a mano armada",
                "Se reportó un robo a mano armada en esta zona. El incidente ocurrió en horario nocturno cuando la víctima caminaba sola.",
                "Av. Corrientes 300, Buenos Aires, Argentina",
                "Hace 2 días",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // Alerta 2: Av. Corrientes 600 - Robo de pertenencias
        crimeAlerts.add(new CrimeAlert(
                "Robo de pertenencias",
                "Robo de celular y billetera reportado por transeúntes. Los delincuentes escaparon en motocicleta.",
                "Av. Corrientes 600, Buenos Aires, Argentina",
                "Hace 1 semana",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // Alerta 3: Av. Corrientes 900 - Intento de robo
        crimeAlerts.add(new CrimeAlert(
                "Intento de robo",
                "Intento de robo frustrado gracias a la intervención de transeúntes. Se recomienda precaución en la zona.",
                "Av. Corrientes 900, Buenos Aires, Argentina",
                "Hace 4 días",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // Alerta 4: Florida 300 - Arrebato
        crimeAlerts.add(new CrimeAlert(
                "Arrebato de cartera",
                "Arrebato de cartera en la zona peatonal durante el horario comercial. La víctima reportó que fueron dos personas en bicicleta.",
                "Florida 300, Buenos Aires, Argentina",
                "Hace 3 días",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // Alerta 5: Sarmiento 500 - Robo con intimidación
        crimeAlerts.add(new CrimeAlert(
                "Robo en taxi",
                "Robo en el interior de un taxi. El conductor era cómplice del hecho. Se recomienda usar aplicaciones de transporte verificadas.",
                "Sarmiento 500, Buenos Aires, Argentina",
                "Hace 1 día",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // Alerta 6: Lavalle 600 - Hurto
        crimeAlerts.add(new CrimeAlert(
                "Hurto por distracción",
                "Hurto de billetera mediante distracción en zona comercial. Los delincuentes operaban en grupo fingiendo ser compradores.",
                "Lavalle 600, Buenos Aires, Argentina",
                "Hace 5 días",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Hurto",
                1
        ));

        // Alerta 7: Av. Santa Fe 800 - Robo con intimidación
        crimeAlerts.add(new CrimeAlert(
                "Robo nocturno",
                "Robo con intimidación en parada de colectivo durante la madrugada. Se llevaron teléfono y documentos.",
                "Av. Santa Fe 800, Buenos Aires, Argentina",
                "Hace 1 semana",
                "Crimen en vía pública",
                "Delitos contra las personas",
                "Robo/Arrebato",
                2
        ));

        // ========== DELITOS CONTRA LA PROPIEDAD (VEHÍCULOS) ==========
        // Casos de robo de vehículos en la zona

        // Robo de vehículo 1: Av. Corrientes 450 - Robo vehículo estacionado
        crimeAlerts.add(new CrimeAlert(
                "Robo de automóvil",
                "Robo de vehículo Toyota Corolla blanco en estacionamiento. Los delincuentes forzaron la cerradura y se llevaron el auto en menos de 3 minutos.",
                "Av. Corrientes 450, Buenos Aires, Argentina",
                "Hace 2 días",
                "Robo de vehículos",
                "Delitos contra la propiedad",
                "Robo vehículo estacionado",
                3
        ));

        // Robo de vehículo 2: Lavalle 500 - Robo pertenencias de vehículo
        crimeAlerts.add(new CrimeAlert(
                "Robo de bicicleta",
                "Robo de bicicleta de alta gama en plena calle peatonal. Los delincuentes cortaron la cadena de seguridad con herramientas especializadas.",
                "Lavalle 500, Buenos Aires, Argentina",
                "Hace 3 días",
                "Robo de vehículos",
                "Delitos contra la propiedad",
                "Robo pertenencias de vehículo",
                2
        ));

        // Robo de vehículo 3: Av. Santa Fe 750 - Robo vehículo estacionado
        crimeAlerts.add(new CrimeAlert(
                "Robo de camioneta",
                "Sustracción de camioneta Ford EcoSport del estacionamiento de un centro comercial. Los ladrones utilizaron inhibidores de alarma.",
                "Av. Santa Fe 750, Buenos Aires, Argentina",
                "Hace 6 días",
                "Robo de vehículos",
                "Delitos contra la propiedad",
                "Robo vehículo estacionado",
                3
        ));

        // Robo de vehículo 4: Av. Corrientes 750 - Robo armado
        crimeAlerts.add(new CrimeAlert(
                "Robo de vehículo a mano armada",
                "Robo de scooter eléctrico a mano armada mientras el propietario realizaba una entrega. Los delincuentes actuaron en grupo con armas de fuego.",
                "Av. Corrientes 750, Buenos Aires, Argentina",
                "Hace 5 días",
                "Robo de vehículos",
                "Delitos contra la propiedad",
                "Robo armado",
                4
        ));
    }

    private void addCrimeAlertsToMap() {
        // Geocodificar las direcciones de las alertas en un hilo separado
        new Thread(() -> {
            try {
                for (CrimeAlert alert : crimeAlerts) {
                    // Geocodificar la dirección usando la misma API que usamos para las rutas
                    GeoPoint location = getGeoPointFromAddress(alert.address);
                    if (location != null) {
                        alert.location = location;
                    }
                }
                
                // Una vez que tenemos todas las ubicaciones, agregar los marcadores en el hilo principal
                runOnUiThread(() -> {
                    // Agregar marcadores de crímenes ACTIVOS
                    for (CrimeAlert alert : crimeAlerts) {
                        if (alert.location != null) { // Solo agregar si se pudo geocodificar
                            Marker marker = createCrimeMarker(alert, true); // true = activo
                            map.getOverlays().add(marker);
                            crimeAlertMarkers.add(marker);
                            startCrimeAlertAnimation(marker);
                        }
                    }
                    
                    // Agregar marcadores de crímenes INACTIVOS
                    for (CrimeAlert alert : inactiveCrimeAlerts) {
                        if (alert.location != null) {
                            Marker marker = createCrimeMarker(alert, false); // false = inactivo
                            map.getOverlays().add(marker);
                            crimeAlertMarkers.add(marker);
                            startCrimeAlertAnimation(marker);
                        }
                    }
                    
                    map.invalidate();
                    
                    // Centrar el mapa en la zona de las alertas si se geocodificaron correctamente
                    centerMapOnAlerts();
                    
                    // Crear zonas de calor DESPUÉS de geocodificar las alertas
                    createDangerZones();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this, "Error al geocodificar las alertas de seguridad", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
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
                        "🕒 Cuándo: " + alert.timeAgo + "\n\n" +
                        "📝 Detalles: " + alert.description + "\n\n";
        
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
        builder.setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss());
        builder.setIcon(R.drawable.ic_alert_warning);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Método eliminado - ya no se usa el botón amarillo para mostrar/ocultar todas las zonas

    private void createDangerZones() {
        // Primero, limpiar zonas existentes
        hideDangerZones();
        
        // Crear zonas individuales para cada crimen basadas en su gravedad
        for (CrimeAlert crime : crimeAlerts) {
            if (crime.location != null) {
                // Usar la gravedad del crimen directamente (1-4)
                createDangerZone(crime.location, crime.severity);
            }
        }
        
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
        
        // Determinar color, tamaño y transparencia basado en la gravedad del crimen (1-4)
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
        
        // Configurar categorías
        String[] categories = {"Delitos contra las personas", "Delitos contra la propiedad"};
        android.widget.ArrayAdapter<String> categoryAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(categoryAdapter);
        
        // Mapa de subtipos por categoría con sus gravedades
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
        
        builder.setPositiveButton("Reportar", null); // Lo configuraremos después
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Configurar el botón "Reportar" después de mostrar el diálogo
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            System.out.println("\n🟢 BOTÓN PRESIONADO: REPORTAR (INACTIVO)");
            
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
            
            if (time.isEmpty()) {
                timeInput.setError("Por favor, indica cuándo ocurrió");
                return;
            }
            
            // Obtener gravedad del subtipo seleccionado
            Map<String, Integer> subtypes = subtypesByCategoryWithSeverity.get(category);
            int severity = subtypes.get(subtype);
            
            // Crear alerta de crimen INACTIVA (siempre inactiva al reportar)
            CrimeAlert newAlert = new CrimeAlert(
                subtype,
                description.isEmpty() ? "Incidente reportado" : description,
                address,
                time,
                category,
                category,
                subtype,
                severity,
                false // INACTIVO - no afecta las rutas hasta que sea apoyado
            );
            
            // Geocodificar y agregar a la lista de alertas inactivas
            geocodeCrimeAlert(newAlert, false);
            
            dialog.dismiss();
            showReportSuccessDialog(category, subtype, severity);
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
        
        builder.setMessage("Tu reporte ha sido registrado y aparecerá en el mapa.\n\n" +
                          "📋 Resumen del reporte:\n" +
                          "📂 Categoría: " + category + "\n" +
                          "🚨 Tipo: " + subtype + "\n" +
                          severityEmoji + " Gravedad: " + severityText + " (" + severity + "/4)\n\n" +
                          "🟢 ESTADO INICIAL: INACTIVO\n" +
                          "• Se mostrará con triángulo verde\n" +
                          "• NO afectará las rutas hasta ser verificado\n" +
                          "• Otros usuarios podrán apoyar o dudar del reporte\n\n" +
                          "💡 Si otros usuarios apoyan tu reporte, se activará\n" +
                          "automáticamente y comenzará a afectar las rutas.\n\n" +
                          "🔒 Toda la información es confidencial.");
        builder.setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss());
        builder.setIcon(R.drawable.ic_inactive_crime);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void geocodeCrimeAlert(CrimeAlert alert, boolean isActive) {
        System.out.println("\n🌍 GEOCODIFICANDO CRIMEN: " + alert.title);
        System.out.println("   Estado solicitado: " + (isActive ? "ACTIVO" : "INACTIVO"));
        System.out.println("   Dirección: " + alert.address);
        
        new Thread(() -> {
            try {
                GeoPoint location = getGeoPointFromAddress(alert.address);
                if (location != null) {
                    alert.location = location;
                    System.out.println("   ✓ Ubicación obtenida: " + location.getLatitude() + ", " + location.getLongitude());
                    
                    // Agregar a la lista correspondiente
                    if (isActive) {
                        crimeAlerts.add(alert);
                        System.out.println("   ✓ Agregado a crimeAlerts (ACTIVOS). Total activos: " + crimeAlerts.size());
                    } else {
                        inactiveCrimeAlerts.add(alert);
                        System.out.println("   ✓ Agregado a inactiveCrimeAlerts (INACTIVOS). Total inactivos: " + inactiveCrimeAlerts.size());
                    }
                    
                    runOnUiThread(() -> {
                        System.out.println("   📍 Creando marcador en UI thread...");
                        
                        // Agregar marcador en el mapa
                        Marker marker = createCrimeMarker(alert, isActive);
                        map.getOverlays().add(marker);
                        crimeAlertMarkers.add(marker);
                        
                        System.out.println("   ✓ Marcador agregado. Total marcadores: " + crimeAlertMarkers.size());
                        
                        // Si es activo, crear zona de peligro
                        if (isActive) {
                            createDangerZone(alert.location, alert.severity);
                            System.out.println("   🔴 Zona de peligro creada (ACTIVO)");
                        } else {
                            System.out.println("   🟢 Sin zona de peligro (INACTIVO)");
                        }
                        
                        // Animar el marcador
                        startCrimeAlertAnimation(marker);
                        
                        // Centrar el mapa en el nuevo marcador
                        IMapController mapController = map.getController();
                        mapController.animateTo(alert.location);
                        
                        map.invalidate();
                        
                        String message = isActive ? 
                            "✅ Alerta ACTIVA agregada (afecta rutas)" : 
                            "✅ Alerta INACTIVA agregada (no afecta rutas)";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        
                        System.out.println("   ✅ PROCESO COMPLETO\n");
                    });
                } else {
                    System.out.println("   ✗ No se pudo geocodificar");
                    runOnUiThread(() -> 
                        Toast.makeText(MainActivity.this, 
                            "❌ No se pudo geocodificar la dirección", 
                            Toast.LENGTH_LONG).show()
                    );
                }
            } catch (Exception e) {
                System.out.println("   ✗ ERROR: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this, 
                        "❌ Error al geocodificar: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    private Marker createCrimeMarker(CrimeAlert alert, boolean isActive) {
        System.out.println("🔧 Creando marcador: " + alert.title + " | Activo: " + isActive);
        
        Marker marker = new Marker(map);
        marker.setPosition(alert.location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        if (isActive) {
            System.out.println("  ➡️ Marcador ACTIVO: ícono rojo/violeta");
            // Marcador rojo para crímenes activos
            marker.setTitle("⚠️ " + alert.title);
            marker.setSnippet(alert.timeAgo + " - " + alert.crimeType);
            marker.setSubDescription(alert.description + "\n" + alert.timeAgo);
            
            Drawable alertIcon = ContextCompat.getDrawable(this, R.drawable.ic_alert_warning);
            if ("Delitos contra la propiedad".equals(alert.category)) {
                // Robos de vehículos - Color violeta
                if (alertIcon != null) {
                    alertIcon.setTint(Color.parseColor("#9C27B0")); // Violeta
                }
            } else {
                // Delitos contra las personas - Color rojo
                if (alertIcon != null) {
                    alertIcon.setTint(Color.parseColor("#F44336")); // Rojo
                }
            }
            marker.setIcon(alertIcon);
            
            // Click listener para mostrar diálogo
            marker.setOnMarkerClickListener((marker1, mapView) -> {
                showCrimeAlertDialog(alert);
                return true;
            });
        } else {
            System.out.println("  ➡️ Marcador INACTIVO: triángulo verde");
            
            // Triángulo verde para crímenes inactivos
            marker.setTitle("📍 " + alert.title + " (Inactivo)");
            marker.setSnippet(alert.timeAgo + " - Inactivo");
            marker.setSubDescription(alert.description + "\n" + alert.timeAgo + "\n\n⚪ Este incidente NO afecta las rutas");
            
            Drawable inactiveIcon = ContextCompat.getDrawable(this, R.drawable.ic_inactive_crime);
            System.out.println("  🎨 Ícono inactivo cargado: " + (inactiveIcon != null ? "✓" : "✗"));
            marker.setIcon(inactiveIcon);
            
            // Click listener para mostrar diálogo
            marker.setOnMarkerClickListener((marker1, mapView) -> {
                showInactiveCrimeDialog(alert);
                return true;
            });
        }
        
        return marker;
    }
    
    private void showInactiveCrimeDialog(CrimeAlert alert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📍 Incidente Reportado (Pendiente de Verificación)");
        
        String severityEmoji;
        String severityText;
        switch (alert.severity) {
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
        
        builder.setMessage(
            "🚨 Tipo: " + alert.subType + "\n" +
            severityEmoji + " Gravedad: " + severityText + " (" + alert.severity + "/4)\n" +
            "📍 Ubicación: " + alert.address + "\n" +
            "🕐 " + alert.timeAgo + "\n\n" +
            "📝 Descripción:\n" + alert.description + "\n\n" +
            "⚪ ESTADO: INACTIVO (Pendiente)\n" +
            "Este incidente fue reportado pero aún no está verificado.\n" +
            "Actualmente NO afecta el cálculo de rutas.\n\n" +
            "¿Qué deseas hacer con este reporte?"
        );
        
        builder.setPositiveButton("✅ Apoyar", (dialog, which) -> {
            activateCrimeAlert(alert);
            dialog.dismiss();
        });
        
        builder.setNegativeButton("❌ Dudar", (dialog, which) -> {
            removeCrimeAlert(alert);
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Cancelar", (dialog, which) -> dialog.dismiss());
        
        builder.setIcon(R.drawable.ic_inactive_crime);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void activateCrimeAlert(CrimeAlert alert) {
        // Remover de la lista de inactivos
        inactiveCrimeAlerts.remove(alert);
        
        // Crear nueva alerta ACTIVA con los mismos datos
        CrimeAlert activeAlert = new CrimeAlert(
            alert.title,
            alert.description,
            alert.address,
            alert.timeAgo,
            alert.crimeType,
            alert.category,
            alert.subType,
            alert.severity,
            true // ACTIVO
        );
        activeAlert.location = alert.location; // Mantener la ubicación geocodificada
        
        // Agregar a la lista de activos
        crimeAlerts.add(activeAlert);
        
        // Actualizar el mapa
        refreshCrimeMarkers();
        
        // Crear zona de peligro
        createDangerZone(activeAlert.location, activeAlert.severity);
        
        // ⭐ RECOMPENSAR AL USUARIO CON 100 PUNTOS
        addPoints(100);
        
        Toast.makeText(this, "✅ Incidente activado - Ahora afecta las rutas seguras", Toast.LENGTH_LONG).show();
    }
    
    private void removeCrimeAlert(CrimeAlert alert) {
        // Remover de la lista correspondiente
        inactiveCrimeAlerts.remove(alert);
        
        // Actualizar el mapa
        refreshCrimeMarkers();
        
        Toast.makeText(this, "❌ Incidente eliminado", Toast.LENGTH_SHORT).show();
    }
    
    private void refreshCrimeMarkers() {
        // Limpiar todos los marcadores de crímenes
        for (Marker marker : crimeAlertMarkers) {
            map.getOverlays().remove(marker);
        }
        crimeAlertMarkers.clear();
        
        // Limpiar zonas de peligro
        hideDangerZones();
        
        // Volver a agregar marcadores activos
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location != null) {
                Marker marker = createCrimeMarker(alert, true);
                map.getOverlays().add(marker);
                crimeAlertMarkers.add(marker);
                startCrimeAlertAnimation(marker);
            }
        }
        
        // Volver a agregar marcadores inactivos
        for (CrimeAlert alert : inactiveCrimeAlerts) {
            if (alert.location != null) {
                Marker marker = createCrimeMarker(alert, false);
                map.getOverlays().add(marker);
                crimeAlertMarkers.add(marker);
                startCrimeAlertAnimation(marker);
            }
        }
        
        // Recrear zonas de peligro solo para activos
        createDangerZones();
        
        map.invalidate();
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
    
    private void refreshCrimeDisplay() {
        // Limpiar overlays existentes de crímenes
        map.getOverlays().removeIf(overlay -> overlay instanceof Marker && 
            ((Marker) overlay).getTitle() != null && 
            (((Marker) overlay).getTitle().contains("ALERTA:") || 
             ((Marker) overlay).getTitle().contains("ROBO:")));
        
        // Limpiar zonas de peligro existentes
        map.getOverlays().removeIf(overlay -> overlay instanceof Polygon);
        
        // Volver a agregar según los filtros activos
        addFilteredCrimeAlertsToMap();
        if (showStreetCrime || showVehicleCrime) {
            createFilteredDangerZones();
        }
        
        map.invalidate(); // Refrescar el mapa
    }
    
    private void addFilteredCrimeAlertsToMap() {
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location == null) continue;
            
            boolean shouldShow = false;
            if ("Crimen en vía pública".equals(alert.crimeType) && showStreetCrime) {
                shouldShow = true;
            } else if ("Robo de vehículos".equals(alert.crimeType) && showVehicleCrime) {
                shouldShow = true;
            }
            
            if (shouldShow) {
                Marker marker = new Marker(map);
                marker.setPosition(alert.location);
                
                if ("Crimen en vía pública".equals(alert.crimeType)) {
                    marker.setTitle("ALERTA: " + alert.title);
                    marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_alert_warning));
                } else if ("Robo de vehículos".equals(alert.crimeType)) {
                    marker.setTitle("ROBO: " + alert.title);
                    marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_alert_warning));
                }
                
                marker.setSnippet(alert.description + " - " + alert.timeAgo);
                marker.setOnMarkerClickListener((selectedMarker, mapView) -> {
                    showCrimeAlertDialog(alert);
                    return true;
                });
                
                map.getOverlays().add(marker);
            }
        }
    }
    
    private void createFilteredDangerZones() {
        for (CrimeAlert alert : crimeAlerts) {
            if (alert.location == null) continue;
            
            boolean shouldShow = false;
            if ("Crimen en vía pública".equals(alert.crimeType) && showStreetCrime) {
                shouldShow = true;
            } else if ("Robo de vehículos".equals(alert.crimeType) && showVehicleCrime) {
                shouldShow = true;
            }
            
            if (shouldShow) {
                // Usar la nueva función que considera la gravedad
                createDangerZone(alert.location, alert.severity);
            }
        }
    }

    // ========== SISTEMA DE PUNTOS ==========
    
    private void loadUserPoints() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userPoints = prefs.getInt(KEY_POINTS, 0);
    }
    
    private void saveUserPoints() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_POINTS, userPoints).apply();
    }
    
    private void updatePointsDisplay() {
        pointsTextView.setText("⭐ " + userPoints + " puntos");
    }
    
    private void addPoints(int points) {
        userPoints += points;
        saveUserPoints();
        animatePointsIncrease(points);
        updatePointsDisplay();
    }
    
    private void animatePointsIncrease(int points) {
        // Animación de escala
        pointsTextView.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(200)
            .withEndAction(() -> {
                pointsTextView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
            })
            .start();
        
        // Toast llamativo
        Toast.makeText(this, "🎉 +" + points + " puntos ganados! 🎉", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onBackPressed() {
        // Si hay una ruta activa (panel de información visible), limpiar la ruta
        if (routeInfoLayout.getVisibility() == View.VISIBLE) {
            clearRoute();
        } else {
            // Si no hay ruta activa, comportamiento normal (cerrar app)
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        // Recargar puntos al volver del RewardsActivity
        loadUserPoints();
        updatePointsDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}

