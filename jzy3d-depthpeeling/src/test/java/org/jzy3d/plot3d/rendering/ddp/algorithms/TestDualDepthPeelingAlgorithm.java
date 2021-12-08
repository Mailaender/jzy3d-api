package org.jzy3d.plot3d.rendering.ddp.algorithms;

import org.junit.Test;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.factories.DepthPeelingPainterFactory;
import org.jzy3d.junit.ChartTester;
import org.jzy3d.junit.NativeChartTester;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.ParallelepipedComposite;
import org.jzy3d.plot3d.primitives.ParallelepipedComposite.PolygonType;
import org.jzy3d.plot3d.primitives.PolygonMode;

public class TestDualDepthPeelingAlgorithm {
    @Test
    public void whenTranslucentIntersectingCube_ThenOrderIndependentAlphaIsProcessed() throws InterruptedException {

      AWTChartFactory f = new AWTChartFactory(new DepthPeelingPainterFactory());

      f.getPainterFactory().setOffscreen(600, 600);
      
      Chart chart = f.newChart();

      Coord3d p1 = Coord3d.ORIGIN;
      Coord3d p2 = new Coord3d(0.005f, 0.005f, 0.005f);
      Coord3d p3 = new Coord3d(0.01f, 0.01f, 0.01f);

      cube(chart, 0.01f, p1, Color.BLUE /* no alpha */, Color.BLACK);
      cube(chart, 0.01f, p2, Color.RED.alpha(.5f), Color.BLACK);
      cube(chart, 0.01f, p3, Color.GREEN.alpha(.5f), Color.BLACK);

      chart.open(800, 600);
      
      Thread.sleep(200);
      //chart.getMouse();

      //chart.getScene().getGraph().
      
      NativeChartTester tester = new NativeChartTester();
      tester.setTextInvisible(false);
      tester.assertSimilar(chart, ChartTester.EXPECTED_IMAGE_FOLDER + TestDualDepthPeelingAlgorithm.class.getSimpleName()+".png");


    }
    
    public static void cube(Chart chart, float width, Coord3d position, Color face, Color wireframe) {
      BoundingBox3d bounds =
          new BoundingBox3d(position.x - width / 2, position.x + width / 2, position.y - width / 2,
              position.y + width / 2, position.z - width / 2, position.z + width / 2);
      ParallelepipedComposite p1 = new ParallelepipedComposite(bounds, PolygonType.SIMPLE);
      p1.setPolygonMode(PolygonMode.FRONT_AND_BACK);
      p1.setPolygonOffsetFill(true);
      p1.setColor(face);
      p1.setWireframeColor(wireframe);
      p1.setWireframeDisplayed(true);
      chart.getScene().add(p1);
    }

}