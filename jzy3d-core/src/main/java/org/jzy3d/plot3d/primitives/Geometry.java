package org.jzy3d.plot3d.primitives;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.IMultiColorable;
import org.jzy3d.colors.ISingleColorable;
import org.jzy3d.events.DrawableChangedEvent;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Normal;
import org.jzy3d.maths.Utils;
import org.jzy3d.painters.IPainter;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.transform.Transform;

public abstract class Geometry extends Wireframeable implements ISingleColorable, IMultiColorable {
  public static boolean NORMALIZE_NORMAL = true;
  public static boolean SPLIT_IN_TRIANGLES = true;
  
  /** A flag to show normals for debugging lighting */
  public static boolean SHOW_NORMALS = false;
  public static int NORMAL_LINE_WIDTH = 2;
  public static int NORMAL_POINT_WIDTH = 4;
  public static Color NORMAL_END_COLOR = Color.GRAY.clone();
  public static Color NORMAL_START_COLOR = Color.GRAY.clone(); 
  
  protected PolygonMode polygonMode;
  protected ColorMapper mapper;
  protected List<Point> points;
  protected Color color;
  protected Coord3d center;
  
   
  
  
  /**
   * Initializes an empty {@link Geometry} with face status defaulting to true, and wireframe status
   * defaulting to false.
   */
  public Geometry() {
    super();
    points = new ArrayList<Point>(4);
    bbox = new BoundingBox3d();
    center = new Coord3d();
    polygonMode = PolygonMode.FRONT_AND_BACK;
  }
  
  public Geometry(Point... points) {
    this();
    add(points);
  }
  
  public Geometry(Color wireframeColor, Color faceColor, Coord3d... points) {
    this();
    add(faceColor, points);
    setWireframeColor(wireframeColor);
    setWireframeDisplayed(wireframeColor!=null);
  }

  public Geometry(List<Point> points) {
    this();
    add(points);
  }

  public Geometry(Color wireframeColor, Point... points) {
    this(points);
    setWireframeColor(wireframeColor);
    setWireframeDisplayed(wireframeColor!=null);
  }

  public Geometry(Color wireframeColor, boolean wireframeDisplayed, Point... points) {
    this(points);
    setWireframeColor(wireframeColor);
    setWireframeDisplayed(wireframeDisplayed);
  }

  /* * */

  @Override
  public void draw(IPainter painter) {
    doTransform(painter);

    if (mapper != null)
      mapper.preDraw(this);
    
    if(isReflectLight()) {
      applyMaterial(painter);
    }

    // Draw content of polygon
    drawFace(painter);

    // drawing order is important for EmulGL to cleanly render polygon edges

    // Draw edge of polygon
    drawWireframe(painter);

    if (mapper != null)
      mapper.postDraw(this);

    doDrawBoundsIfDisplayed(painter);
  }

  protected void drawFace(IPainter painter) {
    if (faceDisplayed) {
      painter.glPolygonMode(polygonMode, PolygonFill.FILL);

      if (wireframeDisplayed && polygonWireframeDepthTrick)
        applyDepthRangeForUnderlying(painter); // ENABLE RANGE FOR UNDER

      if (wireframeDisplayed && polygonOffsetFillEnable)
        polygonOffsetFillEnable(painter); // ENABLE OFFSET

      callPointsForFace(painter);

      if (wireframeDisplayed && polygonOffsetFillEnable)
        polygonOffsetFillDisable(painter); // DISABLE OFFSET
      
      if (wireframeDisplayed && polygonWireframeDepthTrick)
        applyDepthRangeDefault(painter); // DISAABLE RANGE FOR UNDER

    }
  }

  protected void drawWireframe(IPainter painter) {
    if (wireframeDisplayed) {
      painter.glPolygonMode(polygonMode, PolygonFill.LINE);

      if (polygonWireframeDepthTrick)
        applyDepthRangeForOverlying(painter); // OVER - enable range

      if (polygonOffsetFillEnable)
        polygonOffsetFillEnable(painter);

      callPointForWireframe(painter);

      if (polygonOffsetFillEnable)
        polygonOffsetFillDisable(painter);
      
      if (polygonWireframeDepthTrick)
        applyDepthRangeDefault(painter); // OVER - disable range

    }
  }

  /**
   * Drawing the point list in wireframe mode
   */
  protected void callPointForWireframe(IPainter painter) {
    if(!isWireframeColorFromPolygonPoints()) {
      painter.color(wireframeColor);
    }
    painter.glLineWidth(getWireframeWidth());
    painter.glBegin_LineLoop(); // changed for JGL as wireframe polygon are transformed to pair of
                                // triangles

    for (Point p : points) {
      if(isWireframeColorFromPolygonPoints()) {
        painter.color(p.rgb);
      }

      painter.vertex(p.xyz, spaceTransformer);
    }
    painter.glEnd();
  }

  
  
  /**
   * Drawing the point list in face mode (polygon content)
   */
  protected void callPointsForFace(IPainter painter) {
    if(SPLIT_IN_TRIANGLES) {

      int triangles = points.size()-2;
  
      Point p1 = points.get(0);
  
      for (int t = 0; t < triangles; t++) {
        Point p2 = points.get(t+1);
        Point p3 = points.get(t+2);
        
        // process normal for lights
        Coord3d normal = null;
        if(isReflectLight()) {
          normal = Normal.compute(p1.xyz, p2.xyz, p3.xyz, NORMALIZE_NORMAL);
          /*if(normal.z<0) {
            normal.mulSelf(-1);
          }*/
        }

        painter.glBegin_Triangle();

        if(normal!=null) {
          painter.normal(normal);
        }

        applyPointOrMapperColor(painter, p1);
        painter.vertex(p1.xyz, spaceTransformer);

        applyPointOrMapperColor(painter, p2);
        painter.vertex(p2.xyz, spaceTransformer);
        
        applyPointOrMapperColor(painter, p3);
        painter.vertex(p3.xyz, spaceTransformer);
        
        painter.glEnd();
        
        if(SHOW_NORMALS) {
          drawTriangleNormal(painter, p1, p2, p3, normal);
        }
        
      }
    }
    else {
      begin(painter); 
       
      // process normal for lights
      Coord3d normal = null;
      if(isReflectLight()) {
        normal = Normal.compute(points, NORMALIZE_NORMAL, false);
        
        /*if(normal.z<0) {
          normal.mulSelf(-1);
        }*/
      }
      
      // invoke points for vertex and color
      for (Point p : points) {
        if (mapper != null) {
          /*Color c = mapper.getColor(p.xyz);
          painter.color(c);
          
          // store this color in case it should be used for drawing
          // the wireframe as stated by setWireframeColorFrom...
          p.rgb = c; */
          applyPointOrMapperColor(painter, p);
        } else {
          painter.color(p.rgb);
        }
        
        if(normal!=null) {
          painter.normal(normal);
        }
        painter.vertex(p.xyz, spaceTransformer);
        
      }
      painter.glEnd();
      
      if(SHOW_NORMALS) {
        drawPolygonNormal(painter, points, normal);
      }
    }
  }

  protected void drawPolygonNormal(IPainter painter, List<Point> points, Coord3d normal) {
    Coord3d mean = new Coord3d();
    
    for(Point p: points) {
      mean.addSelf(p.xyz);
    }
    mean.divSelf(points.size());
    
    Coord3d end = mean.add(normal);
    
    // normal line
    painter.glLineWidth(NORMAL_LINE_WIDTH);
    painter.glBegin_Line();
    painter.color(NORMAL_START_COLOR);
    painter.vertex(mean, spaceTransformer);
    painter.color(NORMAL_END_COLOR);
    painter.vertex(end, spaceTransformer);
    painter.glEnd();
    
    // normal arrow
    painter.glPointSize(NORMAL_POINT_WIDTH);
    painter.glBegin_Point();
    painter.color(NORMAL_START_COLOR);
    painter.vertex(mean, spaceTransformer);
    painter.color(NORMAL_END_COLOR);
    painter.vertex(end, spaceTransformer);
    painter.glEnd();
  }

  protected void drawTriangleNormal(IPainter painter, Point p1, Point p2, Point p3, Coord3d normal) {
    Coord3d mean = new Coord3d();
    mean.addSelf(p1.xyz);
    mean.addSelf(p2.xyz);
    mean.addSelf(p3.xyz);
    mean.divSelf(3);
    
    Coord3d end = mean.add(normal);

    // normal line
    painter.glLineWidth(NORMAL_LINE_WIDTH);
    painter.glBegin_Line();
    painter.color(NORMAL_START_COLOR);
    painter.vertex(mean, spaceTransformer);
    painter.color(NORMAL_END_COLOR);
    painter.vertex(end, spaceTransformer);
    painter.glEnd();
    
    // normal arrows
    painter.glPointSize(NORMAL_POINT_WIDTH);
    painter.glBegin_Point();
    painter.color(NORMAL_START_COLOR);
    painter.vertex(mean, spaceTransformer);
    painter.color(NORMAL_END_COLOR);
    painter.vertex(end, spaceTransformer);
    painter.glEnd();
  }

  protected void applyPointOrMapperColor(IPainter painter, Point p) {
    if (mapper != null) {
      Color c = mapper.getColor(p.xyz);
      painter.color(c);
      
      // store this color in case it should be used for drawing
      // the wireframe as stated by setWireframeColorFrom...
      p.rgb = c; 
    } else {
      painter.color(p.rgb);
    }
  }

  /**
   * Invoke GL begin with the actual geometry type {@link GL#GL_POINTS}, {@link GL#GL_LINES},
   * {@link GL#GL_TRIANGLES}, {@link GL2#GL_POLYGON} ...
   */
  protected abstract void begin(IPainter painter);

  /* DATA */

  public void add(float x, float y, float z) {
    add(new Coord3d(x, y, z));
  }

  public void add(Coord3d coord) {
    add(new Point(coord, wireframeColor), true);
  }
  
  public void add(Coord3d coord, Color color, boolean updateBounds) {
    add(new Point(coord, color), updateBounds);
  }
  
  public void add(Color faceColor, Coord3d... coords) {
    for(Coord3d coord: coords) {
      add(coord, faceColor, false);
    }
    updateBounds();
  }

  public void add(Point point) {
    add(point, true);
  }

  /** Add a point to the polygon. */
  public void add(Point point, boolean updateBounds) {
    points.add(point);
    if (updateBounds) {
      updateBounds();
    }
  }

  public void add(Point... points) {
    for(Point p: points) {
      add(p, false);
    }
    updateBounds();
  }

  public void add(List<Point> points) {
    for(Point p: points) {
      add(p, false);
    }
    updateBounds();
  }

  
  @Override
  public void applyGeometryTransform(Transform transform) {
    for (Point p : points) {
      p.xyz = transform.compute(p.xyz);
    }
    updateBounds();
  }

  @Override
  public void updateBounds() {
    bbox.reset();
    bbox.add(getPoints());

    // recompute center
    center = new Coord3d();
    for (Point p : points)
      center = center.add(p.xyz);
    center = center.div(points.size());
  }

  @Override
  public Coord3d getBarycentre() {
    return center;
  }

  public Point get(int p) {
    return points.get(p);
  }

  public List<Point> getPoints() {
    return points;
  }
  
  public Set<Point> getPointSet(){
    Set<Point> set = new HashSet<>();
    for(Point p: points) {
      set.add(p);
    }
    return set;
  }

  
  public Set<Coord3d> getCoordSet(){
    Set<Coord3d> set = new HashSet<>();
    for(Point p: points) {
      set.add(p.xyz);
    }
    return set;
  }

  public Coord3d[] getCoordArray(){
    Coord3d[] pts = new Coord3d[size()];
    int k = 0;
    for(Point p: getPoints()) {
      pts[k++] = p.xyz;
    }
    return pts;
  }

  
  public int size() {
    return points.size();
  }

  /* DISTANCES */

  @Override
  public double getDistance(Camera camera) {
    return getBarycentre().distance(camera.getEye());
  }

  @Override
  public double getShortestDistance(Camera camera) {
    double min = Float.MAX_VALUE;
    double dist = 0;
    for (Point point : points) {
      dist = point.getDistance(camera);
      if (dist < min)
        min = dist;
    }

    dist = getBarycentre().distance(camera.getEye());
    if (dist < min)
      min = dist;
    return min;
  }

  @Override
  public double getLongestDistance(Camera camera) {
    double max = 0;
    double dist = 0;
    for (Point point : points) {
      dist = point.getDistance(camera);
      if (dist > max)
        max = dist;
    }
    return max;
  }

  /* SETTINGS */

  public PolygonMode getPolygonMode() {
    return polygonMode;
  }

  /**
   * A null polygonMode imply no any call to gl.glPolygonMode(...) at rendering
   */
  public void setPolygonMode(PolygonMode polygonMode) {
    this.polygonMode = polygonMode;
  }

  /* COLOR */

  @Override
  public void setColorMapper(ColorMapper mapper) {
    this.mapper = mapper;

    fireDrawableChanged(new DrawableChangedEvent(this, DrawableChangedEvent.FIELD_COLOR));
  }

  @Override
  public ColorMapper getColorMapper() {
    return mapper;
  }

  @Override
  public void setColor(Color color) {
    this.color = color;

    for (Point p : points)
      p.setColor(color);

    fireDrawableChanged(new DrawableChangedEvent(this, DrawableChangedEvent.FIELD_COLOR));
  }

  @Override
  public Color getColor() {
    return color;
  }

  @Override
  public String toString(int depth) {
    return (Utils.blanks(depth) + "(" + this.getClass().getSimpleName() + ") #points:"
        + points.size());
  }
}
