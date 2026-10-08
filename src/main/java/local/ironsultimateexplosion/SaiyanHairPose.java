package local.ironsultimateexplosion;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Deforms the connected mane from the crown into the torso, in model pixels. */
final class SaiyanHairPose {
    private final Matrix4f rootPose, headPose, inverseHead;
    private final Matrix3f rootNormal;
    private final float half, top, bottom;
    private final Vector3f point=new Vector3f(), root=new Vector3f(), local=new Vector3f();
    private final Vector3f normal=new Vector3f(), turnedNormal=new Vector3f();
    private float wrapSide;
    private final Vector3f[] points={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};
    private final Vector3f[] normals={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};

    SaiyanHairPose(Matrix4f rootPose, Matrix4f headPose, boolean helmet) {
        this.rootPose=rootPose; this.headPose=headPose;
        inverseHead=new Matrix4f(headPose).invert();
        rootNormal=new Matrix3f(rootPose).invert().transpose();
        half=helmet?5.05f:4.5f; top=helmet?-9.05f:-8.5f; bottom=helmet?1.05f:0;
    }

    static float headWeight(float t) {
        float weight=Math.max(0,Math.min(1,1-t/.75f));
        return weight*weight*(3-2*weight);
    }

    void begin(float[][] faces) {
        root.zero();int count=0;
        for(int f=0;f<Math.min(16,faces.length);f++)for(int i=0;i<36;i+=9)
            if(faces[f][i+7]==0) {root.add(faces[f][i],faces[f][i+1],faces[f][i+2]);count++;}
        root.div(count).mulPosition(rootPose);
        wrapSide=root.y>-2 && Math.abs(root.z)<5 && Math.abs(root.x)>6?Math.signum(root.x):0;
    }

    void apply(float[] face,int i,float sway,float ripple,float lift,float reveal) {
        float t=face[i+7], bend=t*t, weight=headWeight(t);
        point.set(face[i]+sway*bend,
            -10.6f+(face[i+1]+10.6f)*(.35f+.65f*reveal)-lift*bend,
            face[i+2]+ripple*bend);
        root.set(face[i],face[i+1],face[i+2]).mulPosition(rootPose);
        point.lerp(root,weight);

        // The anchored section already clears the skull in head coordinates.
        // Never displace it with a torso-space collision plane.
        if(weight<1) {
            clearHead();
            // Only the descending hair clears the body. A turned root remains
            // exactly where the crown is, including looking straight down.
            if(point.y>-3f) {
                // A root beside a raised/turned head routes the entire lock
                // around the same shoulder. Per-vertex side choices can fold
                // one surface across the body between front and back.
                if(wrapSide!=0 && point.z<6) {
                    point.x=wrapSide*Math.max(9.5f,point.x*wrapSide);
                    point.z=Math.max(1.2f,point.z);
                } else {
                    float extent=(float)Math.sqrt(point.x*point.x/256f+point.z*point.z/25f);
                    if(extent<.0001f)point.z=5f;
                    else if(extent<1) { point.x/=extent;point.z/=extent; }
                }
            }
            clearHead();
        }
        normal.set(face[i+3],face[i+4],face[i+5]);
        turnedNormal.set(normal).mul(rootNormal).normalize();
        normal.lerp(turnedNormal,weight).normalize();
    }

    void face(float[] face,float sway,float ripple,float lift,float reveal) {
        float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY;
        float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        for(int v=0;v<4;v++) {
            apply(face,v*9,sway,ripple,lift,reveal);
            points[v].set(point);normals[v].set(normal);
            local.set(point).mulPosition(inverseHead);
            minX=Math.min(minX,local.x);maxX=Math.max(maxX,local.x);
            minY=Math.min(minY,local.y);maxY=Math.max(maxY,local.y);
        }
        // Use one separating plane for the whole surface near the skull.
        // Individually clear corners can still form a triangle through it.
        if(minX<half+.75f && maxX>-half-.75f && minY<bottom+.75f && maxY>top-.75f)
            for(var vertex:points) {
                local.set(vertex).mulPosition(inverseHead);
                if(local.z<half+.15f) {local.z=half+.15f;vertex.set(local).mulPosition(headPose);}
            }
    }

    private void clearHead() {
        local.set(point).mulPosition(inverseHead);
        float gap=Math.max(Math.max(0,Math.abs(local.x)-half-2.5f),
            Math.max(0,Math.max(top-2.5f-local.y,local.y-bottom-2.5f)));
        float influence=1-Math.min(1,gap/2);
        local.z+=Math.max(0,half+.85f-local.z)*influence;
        point.set(local).mulPosition(headPose);
    }

    Vector3f point() { return point; }
    Vector3f normal() { return normal; }
    Vector3f point(int vertex) { return points[vertex]; }
    Vector3f normal(int vertex) { return normals[vertex]; }
}
