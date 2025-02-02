package jogl;

import org.junit.Test;
import org.jzy3d.os.OperatingSystem;
import org.jzy3d.plot3d.GPUInfo;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

/**
 * This shows how to switch OpenGL version with JOGL.
 * 
 * It requires to invoke the JVM with -Djogl.disable.openglcore=true to work.
 * 
 * @see https://github.com/jzy3d/jogl/issues/7
 * @see https://forum.jogamp.org/Selecting-the-highest-possible-GL-profile-at-runtime-td4041302.html
 */
public class Test_OpenGLVersion {
  @Test
  public void openGLversion() throws Exception {
    System.out.println("=============================================================");
    System.out.println("");
    System.out.println("");
    System.out.println("                   OS & JVM VERSION INFO                     ");
    System.out.println("");
    System.out.println("");
    System.out.println("=============================================================");
    
    OperatingSystem os = new OperatingSystem();
    
    System.out.println(os);
    
    
    System.out.println("=============================================================");
    System.out.println("");
    System.out.println("");
    System.out.println("                    OPENGL VERSION INFO                      ");
    System.out.println("");
    System.out.println("");
    System.out.println("=============================================================");
    
    
    // ------------------------------------------------------
    // Profile & capabilities
    
    //GLProfile glp = NativePainterFactory.detectGLProfile(); // use Jzy3D profile selection

    //GLProfile glp = GLProfile.get(GLProfile.GL4);
    GLProfile glp = GLProfile.getMaxProgrammable(true);
    
    GLCapabilities caps = new GLCapabilities(glp);
    caps.setOnscreen(false);

    createGLContextAndPrintInfo(glp, caps);
  }

  public static void createGLContextAndPrintInfo(GLProfile glp, GLCapabilities caps) {
    // ------------------------------------------------------
    // Drawable to get a GL context

    GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
    GLAutoDrawable drawable =
        factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), caps, null, 100, 100);
    drawable.display();
    drawable.getContext().makeCurrent();

    GL gl = drawable.getContext().getGL();


    // ------------------------------------------------------
    // Report
    
    System.out.println("PROFILE       : " + glp);
    System.out.println("CAPS (query)  : " + caps);
    System.out.println("CAPS (found)  : " + drawable.getChosenGLCapabilities());
    
    System.out.println("--------------------------------------------------");
    System.out.println(GPUInfo.load(gl));
    
    System.out.println("--------------------------------------------------");
    System.out.println(drawable.getContext());
    System.out.println();
    System.out.println("Is compat profile : " + drawable.getContext().isGLCompatibilityProfile());
    
    
    
    System.out.println("--------------------------------------------------");
    System.out.println("GL2    : " + GLProfile.isAvailable(GLProfile.GL2));
    System.out.println("GL2GL3 : " + GLProfile.isAvailable(GLProfile.GL2GL3));
    System.out.println("GL3    : " + GLProfile.isAvailable(GLProfile.GL3));
    System.out.println("GL3bc  : " + GLProfile.isAvailable(GLProfile.GL3bc));
    System.out.println("GL4    : " + GLProfile.isAvailable(GLProfile.GL4));
    System.out.println("GL4ES3 : " + GLProfile.isAvailable(GLProfile.GL4ES3));
    System.out.println("GL4bc  : " + GLProfile.isAvailable(GLProfile.GL4bc));
    

    // ------------------------------------------------------
    // Try invoking something

    
    //gl.getGL2().glClear(0);
    //gl.getGL4bc().glClear(0);
    

    // ------------------------------------------------------
    // We are done, release context for further work
    
    drawable.getContext().release();
  }

  public static String getDebugInfo(GL gl) {
    StringBuffer sb = new StringBuffer();
    sb.append("GL_VENDOR     : " + gl.glGetString(GL.GL_VENDOR) + "\n");
    sb.append("GL_RENDERER   : " + gl.glGetString(GL.GL_RENDERER) + "\n");
    sb.append("GL_VERSION    : " + gl.glGetString(GL.GL_VERSION) + "\n");
    
    String ext = gl.glGetString(GL.GL_EXTENSIONS);

    if(ext!=null) {
      String[] exts = ext.split(" ");
      sb.append("GL_EXTENSIONS : (" + exts.length + ")\n");
      /*for(String e: exts) {
        sb.append("\t" + e + "\n");
      }*/
    }
    else {
      sb.append("GL_EXTENSIONS : null\n");      
    }
    
    sb.append("GL INSTANCE : " + gl.getClass().getName() + "\n");
    
    return sb.toString();
  }
  
}
