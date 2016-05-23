package com.wuta.gpuimage;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;
import android.opengl.GLES20;

import com.wuta.gpuimage.util.OpenGlUtils;

/**
 * Created by LW on 2016/5/13.
 */
public class Triangle {
    private int mPositionHandle;
    private int mColorHandle;

    private final int vertexCount ;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private FloatBuffer vertexBuffer;
    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    private final int mProgram;
    float [] triangleCoords;

    // Set color with red, green, blue and alpha (opacity) values
    float[] color ;

    public Triangle(float [] mtriangleCoord, float [] mcolor) {
        // initialize vertex byte buffer for shape coordinates
        triangleCoords = mtriangleCoord;
        color = mcolor;
        vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
        int vertexShader = OpenGlUtils.loadShader(vertexShaderCode,GLES20.GL_VERTEX_SHADER);
        int fragmentShader = OpenGlUtils.loadShader(fragmentShaderCode,GLES20.GL_FRAGMENT_SHADER);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);
    }
    public void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
