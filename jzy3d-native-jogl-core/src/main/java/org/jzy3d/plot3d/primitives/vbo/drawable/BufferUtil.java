package org.jzy3d.plot3d.primitives.vbo.drawable;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.jzy3d.maths.Coord3d;

public class BufferUtil {
  /**
   * A simple utility to upcast buffers to invoke some of their methods without hitting compatibility issues between Java < 9 and Java 9+
   * 
   * https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip
   * 
   * Useful if compiled with Java >9 and app run with Java <9
   */
  public static void rewind(Buffer buffer) {
    buffer.rewind();
  }

  /*public static void rewind(FloatBuffer buffer) {
    buffer.rewind();
  }

  public static void rewind(IntBuffer buffer) {
    buffer.rewind();
  }*/
  
  public static Coord3d getCoordAt(FloatBuffer buffer, int i) {
    return new Coord3d(buffer.get(i), buffer.get(i+1), buffer.get(i+2));
  }

  public static List<Coord3d> getCoords(FloatBuffer buffer) {
    List<Coord3d> c = new ArrayList<>(buffer.capacity()/3);

    for (int j = 0; j < buffer.capacity(); j+=3) {
      c.add(getCoordAt(buffer, j));
    }
    return c;
  }

  /*public static Coord3d getCoordAt(double[] array, int i) {
    return new Coord3d(array[i], array[i+1], array[i+2]);
  }*/


  
}
