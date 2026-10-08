package local.ironsultimateexplosion;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.file.*;
import java.util.*;

/** Exercises the actual render deformation, including the reported 90-degree pitch. */
public class SaiyanAttachedHairCheck {
    static long tested, anchors, normals;static int headCollisions,bodyCollisions;
    static float maximumRootError;static List<String> errors=new ArrayList<>();
    static void fail(String error) {
        String type=error.substring(0,4);
        if(errors.stream().filter(e->e.startsWith(type)).count()<6)errors.add(error);
    }
    static boolean inside(Vector3f p,float half,float top,float bottom) {
        return Math.abs(p.x)<half-.0001f && p.y>top+.0001f && p.y<bottom-.0001f && Math.abs(p.z)<half-.0001f;
    }
    static void checkPoint(Vector3f world,Matrix4f inverseHead,boolean helmet,String pose) {
        Vector3f local=new Vector3f(world).mulPosition(inverseHead);
        if(inside(local,helmet?5.05f:4.5f,helmet?-9.05f:-8.5f,helmet?1.05f:0)) {
            headCollisions++;fail("head "+pose+" "+local);
        }
        if(inside(world,8.25f,0,24) && Math.abs(world.z)<2.3f) {
            bodyCollisions++;fail("body "+pose+" "+world);
        }
        tested++;
    }
    static String facesJson(float[][] faces,SaiyanHairPose deformation,Matrix4f body,Matrix4f head,float reveal,float[] motion) {
        if(deformation!=null)deformation.begin(faces);
        StringBuilder out=new StringBuilder("[");
        for(int f=0;f<faces.length;f++) {
            if(deformation!=null)deformation.face(faces[f],motion[0],motion[1],motion[2],reveal);
            if(f>0)out.append(',');out.append('[');
            for(int i=0;i<36;i+=9) {
                Vector3f p,n;
                if(deformation!=null) { p=new Vector3f(deformation.point(i/9)).mulPosition(body);n=new Vector3f(deformation.normal(i/9)); }
                else {p=new Vector3f(faces[f][i],faces[f][i+1],faces[f][i+2]).mulPosition(head);n=new Vector3f(faces[f][i+3],faces[f][i+4],faces[f][i+5]);}
                float[] values={p.x,p.y,p.z,n.x,n.y,n.z,faces[f][i+6],faces[f][i+7],faces[f][i+8]};
                for(int j=0;j<9;j++) {if(i+j>0)out.append(',');out.append(values[j]);}
            }out.append(']');
        }return out.append(']').toString();
    }
    public static void main(String[] args)throws Exception {
        float[][][] tails=SaiyanHairMesh.thirdTails();
        int[] yaws={-90,-75,-45,-15,0,15,45,75,90},pitches={-90,-75,-60,-30,0,30,60,75,90};
        for(boolean helmet:new boolean[]{false,true}) for(int yaw:yaws) for(int pitch:pitches)
            for(float reveal:new float[]{0,.5f,1}) for(float[] motion:new float[][]{{0,0,0},{5.2f,1,2.2f},{-5.2f,-1,2.2f}}) {
                Matrix4f head=new Matrix4f().rotateY((float)Math.toRadians(yaw)).rotateX((float)Math.toRadians(pitch));
                Matrix4f body=helmet?new Matrix4f().translate(0,-.75f,0).scale(1.12f,1,1.12f):new Matrix4f();
                Matrix4f wigHead=new Matrix4f(head).mul(body),inverseBody=new Matrix4f(body).invert();
                SaiyanHairPose pose=new SaiyanHairPose(new Matrix4f(inverseBody).mul(wigHead),new Matrix4f(inverseBody).mul(head),helmet);
                Matrix4f inverseHead=new Matrix4f(head).invert();
                String label="helmet="+helmet+" yaw="+yaw+" pitch="+pitch+" reveal="+reveal+" sway="+motion[0];
                Vector3f[] points={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};
                Vector3f centre=new Vector3f(),expected=new Vector3f();
                for(float[][] tail:tails) { pose.begin(tail);for(float[] face:tail) {
                    pose.face(face,motion[0],motion[1],motion[2],reveal);
                    centre.zero();
                    for(int v=0;v<4;v++) {
                        int i=v*9;
                        points[v].set(pose.point(v)).mulPosition(body);centre.add(points[v]);
                        checkPoint(points[v],inverseHead,helmet,label+" vertex");
                        if(SaiyanHairPose.headWeight(face[i+7])==1) {
                            expected.set(face[i],face[i+1],face[i+2]).mulPosition(wigHead);
                            float error=expected.distance(points[v]);maximumRootError=Math.max(maximumRootError,error);
                            if(error>.0001f)throw new AssertionError("Root detached: "+error+" "+label);
                            anchors++;
                        }
                        float length=pose.normal(v).length();if(!Float.isFinite(length)||Math.abs(length-1)>.0001f)throw new AssertionError("Invalid normal");normals++;
                    }
                    centre.mul(.25f);checkPoint(centre,inverseHead,helmet,label+" quad");
                    // Interior samples on both triangles catch a face spanning an obstacle.
                    for(int[] tri:new int[][]{{0,1,2},{0,2,3}}) {
                        centre.set(points[tri[0]]).add(points[tri[1]]).add(points[tri[2]]).div(3);checkPoint(centre,inverseHead,helmet,label+" triangle");
                    }
                }}
                if(!helmet&&reveal==1&&motion[0]==0&&((yaw==0&&(pitch==0||Math.abs(pitch)==90))||(Math.abs(yaw)==90&&pitch==0))) {
                    String name="yaw"+yaw+"-pitch"+pitch;
                    StringBuilder mesh=new StringBuilder("[").append(facesJson(SaiyanHairMesh.thirdHead(),null,body,wigHead,reveal,motion));
                    for(float[][] tail:tails)mesh.append(',').append(facesJson(tail,pose,body,wigHead,reveal,motion));
                    Files.writeString(Path.of(args[0],name+".json"),mesh.append(']').toString());
                }
            }
        String result="{\"passed\":"+(headCollisions==0&&bodyCollisions==0)+",\"movingHeadSamples\":"+tested+",\"exactRootAnchors\":"+anchors+",\"maximumRootErrorPixels\":"+maximumRootError+",\"finiteUnitNormals\":"+normals+",\"headCollisionSamples\":"+headCollisions+",\"bodyCollisionSamples\":"+bodyCollisions+"}";
        Files.writeString(Path.of(args[0],"pose-result.json"),result);System.out.println(result);
        if(!errors.isEmpty())throw new AssertionError(String.join("\n",errors));
    }
}
