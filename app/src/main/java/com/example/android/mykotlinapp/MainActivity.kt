package com.example.android.mykotlinapp

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.Menu
import android.view.MenuItem
import com.carto.core.MapPos
import com.carto.core.MapPosVector
import com.carto.core.MapPosVectorVector
import com.carto.core.Variant
import com.carto.datasources.LocalVectorDataSource
import com.carto.graphics.Color
import com.carto.layers.CartoBaseMapStyle
import com.carto.layers.CartoOnlineVectorTileLayer
import com.carto.layers.VectorLayer
import com.carto.projections.Projection
import com.carto.styles.LineJoinType
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyleBuilder
import com.carto.styles.PointStyleBuilder
import com.carto.styles.PolygonStyleBuilder
import com.carto.ui.MapView
import com.carto.vectorelements.Line
import com.carto.vectorelements.Marker
import com.carto.vectorelements.Point
import com.carto.vectorelements.Polygon

class MainActivity : AppCompatActivity() {
    val LICENSE = "XTUN3Q0ZCdkxrMUJlazZHYm1GOHc0SUFmWkxDVHFRcEFBaFE3MXcyajMzL21oSVF5Tk5uR0Z4RGlseEhXbVE9PQoKYXBwVG9rZW49YTViNTM1YWUtYTkxZS00Zjc1LWI5YmMtNGNiNTNhZDM0ZDcwCnBhY2thZ2VOYW1lPWNvbS5leGFtcGxlLmFuZHJvaWQubXlrb3RsaW5hcHAKb25saW5lTGljZW5zZT0xCnByb2R1Y3RzPXNkay1hbmRyb2lkLTQuKgp3YXRlcm1hcms9Y2FydG9kYgo="

    var mapView: MapView? = null
    var projection: Projection? = null
    var source: LocalVectorDataSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapView.registerLicense(LICENSE, this)

        mapView = MapView(this)
        setContentView(mapView)

        // Create basemap layer with bundled style
        val basemapLayer = CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_DEFAULT)
        // Add layer to map
        mapView?.layers?.add(basemapLayer)

        // Get base projection from mapView
        projection = mapView?.options?.baseProjection
        // Create a local vector data source
        source = LocalVectorDataSource(projection)
        // Initialize layer
        val layer = VectorLayer(source)
        mapView?.layers?.add(layer)

        // 1. Position in latitude/longitude has to be converted using projection
        // 2. X is longitude, Y is latitude
        val tallinn = projection?.fromWgs84(MapPos(24.646469, 59.426939))

        addLine(tallinn)
        addPoint(tallinn)
        addMarker(tallinn)
        addPolygon()
    }

    fun addMarker(position: MapPos?) {

        val builder = MarkerStyleBuilder()
        builder.size = 30F
        val red = Color(255, 0, 0, 255)
        builder.color = red

        val style = builder.buildStyle()

        val marker = Marker(position, style)

        // The defined metadata will be used later for Popups
        marker.setMetaDataElement("ClickText", Variant("Marker"))
        source?.add(marker)

        mapView?.setFocusPos(position, 0F)
        mapView?.setZoom(12F, 1F)

    }

    fun addPoint(position: MapPos?) {

        // 1. Create style and position for the Point
        val builder = PointStyleBuilder()
        builder.color = Color(0, 255, 0, 255)
        builder.size = 16F

        // 2. Create Point, add to datasource with metadata
        val point1 = Point(position, builder.buildStyle())
        point1.setMetaDataElement("ClickText", Variant("Point nr 1"))

        source?.add(point1)

        // 4. Animate map to the point
        mapView?.setFocusPos(position, 1F)
        mapView?.setZoom(12F, 1F)
    }

    fun addLine(position: MapPos?) {

        // 1. Create line style, and line poses
        val lineStyleBuilder = LineStyleBuilder()
        lineStyleBuilder.color = Color(255, 0, 0, 255)
        // Define how lines are joined
        lineStyleBuilder.lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND
        lineStyleBuilder.width = 8F

        // 2. Special MapPosVector must be used for coordinates
        val linePoses = MapPosVector()
        val initial = projection?.fromWgs84(MapPos(24.645565, 59.422074))

        // 3. Add positions
        linePoses.add(initial)
        linePoses.add(projection?.fromWgs84(MapPos(24.643076, 59.420502)));
        linePoses.add(projection?.fromWgs84(MapPos(24.645351, 59.419149)));
        linePoses.add(projection?.fromWgs84(MapPos(24.648956, 59.420393)));
        linePoses.add(projection?.fromWgs84(MapPos(24.650887, 59.422707)));

        // 4. Add a line
        val line1 = Line(linePoses, lineStyleBuilder.buildStyle());
        line1.setMetaDataElement("ClickText", Variant("Line nr 1"))
        source?.add(line1)

        // 5. Animate map to the line
        mapView?.setFocusPos(position, 1F)
        mapView?.setZoom(12F, 1F)
    }

    fun addPolygon() {

        // 1. Create polygon style and poses
        val polygonStyleBuilder = PolygonStyleBuilder()
        polygonStyleBuilder.color = Color(0xFFFF0000.toInt())

        val lineStyleBuilder = LineStyleBuilder()
        // Blue
        lineStyleBuilder.color = Color(0, 0, 255, 255)
        lineStyleBuilder.width = 1F

        polygonStyleBuilder.lineStyle = lineStyleBuilder.buildStyle()

        val polygonPoses = MapPosVector()
        val initial = projection?.fromWgs84(MapPos(24.650930, 59.421659))
        polygonPoses.add(initial)

        polygonPoses.add(projection?.fromWgs84(MapPos(24.657453, 59.416354)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.661187, 59.414607)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.667667, 59.418123)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.665736, 59.421703)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.661444, 59.421245)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.660199, 59.420677)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.656552, 59.420175)))
        polygonPoses.add(projection?.fromWgs84(MapPos(24.654010, 59.421472)))

        // 2. Create 2 polygon holes
        val holePoses1 = MapPosVector()
        holePoses1.add(projection?.fromWgs84(MapPos(24.658409, 59.420522)))
        holePoses1.add(projection?.fromWgs84(MapPos(24.662207, 59.418896)))
        holePoses1.add(projection?.fromWgs84(MapPos(24.662207, 59.417411)))
        holePoses1.add(projection?.fromWgs84(MapPos(24.659524, 59.417171)))
        holePoses1.add(projection?.fromWgs84(MapPos(24.657615, 59.419834)))
        val holePoses2 = MapPosVector()
        holePoses2.add(projection?.fromWgs84(MapPos(24.665640, 59.421243)))
        holePoses2.add(projection?.fromWgs84(MapPos(24.668923, 59.419463)))
        holePoses2.add(projection?.fromWgs84(MapPos(24.662893, 59.419365)))
        val polygonHoles = MapPosVectorVector()

        polygonHoles.add(holePoses1)
        polygonHoles.add(holePoses2)

        // 3. Add polygon
        val polygon = Polygon(polygonPoses, polygonHoles, polygonStyleBuilder.buildStyle())
        polygon.setMetaDataElement("ClickText", Variant("Polygon"))
        source?.add(polygon)

        // 4. Animate zoom to position
        mapView?.setFocusPos(initial, 1F)
        mapView?.setZoom(13F, 1F)
    }
}
