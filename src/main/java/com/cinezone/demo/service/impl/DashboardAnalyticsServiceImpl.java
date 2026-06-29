package com.cinezone.demo.service.impl;

import com.cinezone.demo.service.DashboardAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class DashboardAnalyticsServiceImpl implements DashboardAnalyticsService {

    @Override
    public List<Map<String, Object>> getWaterfallData() {
        return Arrays.asList(
                Map.of("name", "Ingresos Brutos", "value", 1000000),
                Map.of("name", "Descuentos", "value", -150000),
                Map.of("name", "Impuestos", "value", -200000),
                Map.of("name", "Ingresos Netos", "value", 650000)
        );
    }

    @Override
    public List<Map<String, Object>> getMapaCalorData() {
        return Arrays.asList(
                Map.of("dia", "Lunes", "hora", "18:00", "valor", 40),
                Map.of("dia", "Lunes", "hora", "20:00", "valor", 60),
                Map.of("dia", "Viernes", "hora", "18:00", "valor", 85),
                Map.of("dia", "Viernes", "hora", "20:00", "valor", 95),
                Map.of("dia", "Sábado", "hora", "20:00", "valor", 100)
        );
    }

    @Override
    public List<Map<String, Object>> getForecastingData() {
        return Arrays.asList(
                Map.of("mes", "Ene", "real", 120000, "proyectado", 115000),
                Map.of("mes", "Feb", "real", 130000, "proyectado", 125000),
                Map.of("mes", "Mar", "real", 0, "proyectado", 140000)
        );
    }

    @Override
    public Map<String, Object> getLtvData() {
        return Map.of(
                "ltvPromedio", 350.50,
                "crecimientoMensual", 5.2,
                "segmentos", Arrays.asList(
                        Map.of("segmento", "Gold", "valor", 500),
                        Map.of("segmento", "Silver", "valor", 250),
                        Map.of("segmento", "Regular", "valor", 100)
                )
        );
    }

    @Override
    public List<Map<String, Object>> getTreemapDulceriaData() {
        return Arrays.asList(
                Map.of("name", "Popcorn", "value", 45000),
                Map.of("name", "Bebidas", "value", 35000),
                Map.of("name", "Chocolates", "value", 15000),
                Map.of("name", "Nachos", "value", 20000)
        );
    }

    @Override
    public List<Map<String, Object>> getStackedBarData() {
        return Arrays.asList(
                Map.of("mes", "Ene", "taquilla", 50000, "dulceria", 30000, "eventos", 5000),
                Map.of("mes", "Feb", "taquilla", 55000, "dulceria", 32000, "eventos", 6000),
                Map.of("mes", "Mar", "taquilla", 60000, "dulceria", 35000, "eventos", 8000)
        );
    }

    @Override
    public List<Map<String, Object>> getHeatmapAfluenciaData() {
        return Arrays.asList(
                Map.of("zona", "Lobby", "hora", "18:00", "densidad", 80),
                Map.of("zona", "Lobby", "hora", "20:00", "densidad", 95),
                Map.of("zona", "Pasillos", "hora", "18:00", "densidad", 60),
                Map.of("zona", "Baños", "hora", "20:00", "densidad", 40)
        );
    }

    @Override
    public Map<String, Object> getMetaMensualData() {
        return Map.of(
                "meta", 200000,
                "actual", 150000,
                "porcentaje", 75
        );
    }

    @Override
    public List<Map<String, Object>> getRankingOcupacionData() {
        return Arrays.asList(
                Map.of("pelicula", "Avengers", "ocupacion", 95),
                Map.of("pelicula", "Star Wars", "ocupacion", 85),
                Map.of("pelicula", "Matrix", "ocupacion", 75)
        );
    }

    @Override
    public List<Map<String, Object>> getNominaVsIngresosData() {
        return Arrays.asList(
                Map.of("semana", "Semana 1", "nomina", 10000, "ingresos", 50000),
                Map.of("semana", "Semana 2", "nomina", 10000, "ingresos", 55000),
                Map.of("semana", "Semana 3", "nomina", 12000, "ingresos", 60000)
        );
    }

    @Override
    public List<Map<String, Object>> getEstadoSalasData() {
        return Arrays.asList(
                Map.of("sala", "Sala 1", "estado", "Operativa", "limpieza", "Pendiente"),
                Map.of("sala", "Sala 2", "estado", "Operativa", "limpieza", "N/A"),
                Map.of("sala", "Sala 3", "estado", "Operativa", "limpieza", "Lista")
        );
    }

    @Override
    public List<Map<String, Object>> getInsumosCriticosData() {
        return Arrays.asList(
                Map.of("insumo", "Maíz Pira", "stock", 15, "minimo", 20, "estado", "Crítico"),
                Map.of("insumo", "Vasos Grandes", "stock", 50, "minimo", 100, "estado", "Crítico"),
                Map.of("insumo", "Sirope Cola", "stock", 5, "minimo", 10, "estado", "Alerta")
        );
    }

    @Override
    public List<Map<String, Object>> getTiemposColaData() {
        return Arrays.asList(
                Map.of("area", "Taquilla", "tiempoPromedioMin", 5),
                Map.of("area", "Dulcería", "tiempoPromedioMin", 12),
                Map.of("area", "Ingreso Salas", "tiempoPromedioMin", 3)
        );
    }

    @Override
    public List<Map<String, Object>> getKanbanReclamosData() {
        return Arrays.asList(
                Map.of("id", "REC-001", "estado", "Pendiente", "prioridad", "Alta", "descripcion", "Aire acondicionado Sala 2"),
                Map.of("id", "REC-002", "estado", "En Proceso", "prioridad", "Media", "descripcion", "Silla rota Sala 1"),
                Map.of("id", "REC-003", "estado", "Resuelto", "prioridad", "Baja", "descripcion", "Lámpara fundida pasillo")
        );
    }
}
