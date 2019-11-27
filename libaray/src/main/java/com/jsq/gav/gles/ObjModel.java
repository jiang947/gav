package com.jsq.gav.gles;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by jiang on 2019/7/16
 * 3d obj model
 * 格式说明:
 * <p>
 * 顶点数据(Vertex data)：
 * 　　v  几何体顶点 (Geometric vertices)
 * 　　vt 贴图坐标点 (Texture vertices)
 * 　　vn 顶点法线 (Vertex normals)
 * 　　vp 参数空格顶点 (Parameter space vertices)
 * <p>
 * 自由形态曲线(Free-form curve)/表面属性(surface attributes):
 * 　　deg 度 (Degree)
 * 　　bmat 基础矩阵 (Basis matrix)
 * 　　step 步尺寸 (Step size)
 * 　　cstype 曲线或表面类型 (Curve or surface type)
 * <p>
 * 元素(Elements):
 * 　　p 点 (Point)
 * 　　l 线 (Line)
 * 　　f 面 (Face)
 * 　　curv 曲线 (Curve)
 * 　　curv2 2D曲线 (2D curve)
 * 　　surf 表面 (Surface)
 * <p>
 * 自由形态曲线(Free-form curve)/表面主体陈述(surface body statements):
 * 　　parm 参数值 (Parameter values )
 * 　　trim 外部修剪循环 (Outer trimming loop)
 * 　　hole 内部整修循环 (Inner trimming loop)
 * 　　scrv 特殊曲线 (Special curve)
 * 　　sp 特殊的点 (Special point)
 * 　　end 结束陈述 (End statement)
 * <p>
 * 自由形态表面之间的连接(Connectivity between free-form surfaces):
 * 　　con 连接 (Connect)
 * <p>
 * 成组(Grouping):
 * 　　g 组名称 (Group name)
 * 　　s 光滑组 (Smoothing group)
 * 　　mg 合并组 (Merging group)
 * 　　o 对象名称 (Object name)
 * <p>
 * 显示(Display)/渲染属性(render attributes):
 * 　　bevel 导角插值 (Bevel interpolation)
 * 　　c_interp 颜色插值 (Color interpolation)
 * 　　d_interp 溶解插值 (Dissolve interpolation)
 * 　　lod 细节层次 (Level of detail)
 * 　　usemtl 材质名称 (Material name)
 * 　　mtllib 材质库 (Material library)
 * 　　shadow_obj 投射阴影 (Shadow casting)
 * 　　trace_obj 光线跟踪 (Ray tracing)
 * 　　ctech 曲线近似技术 (Curve approximation technique)
 * 　　stech 表面近似技术 (Surface approximation technique)
 */

public class ObjModel {

    private Buffer mPosVertices; // 顶点数据
    private Buffer mTexVertices; // 纹理数据
    private Buffer mNorVertices; // normals 法线数据

    private int mPosVertexCount; // PosVertices count

    private MtlModel mMtl;

    private ObjModel() {
    }


    public static ObjModel read(InputStream stream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(stream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            ArrayList<Float> alv = new ArrayList<>();//原始顶点坐标列表
            ArrayList<Float> alvResult = new ArrayList<>();//结果顶点坐标列表
            ArrayList<Float> norlArr = new ArrayList<>();
            float[] ab = new float[3], bc = new float[3], norl = new float[3];
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] tmp = line.split("[ ]+");
                if (tmp[0].trim().equals("v")) { // 几何体顶点 (Geometric vertices)
                    alv.add(Float.parseFloat(tmp[1]));
                    alv.add(Float.parseFloat(tmp[2]));
                    alv.add(Float.parseFloat(tmp[3]));
                } else if (tmp[0].trim().equals("f")) {
                    int a = Integer.parseInt(tmp[1]) - 1;
                    int b = Integer.parseInt(tmp[2]) - 1;
                    int c = Integer.parseInt(tmp[3]) - 1;
                    int d = Integer.parseInt(tmp[4]) - 1;

                    alvResult.add(alv.get(a * 3));
                    alvResult.add(alv.get(a * 3 + 1));
                    alvResult.add(alv.get(a * 3 + 2));
                    alvResult.add(alv.get(b * 3));
                    alvResult.add(alv.get(b * 3 + 1));
                    alvResult.add(alv.get(b * 3 + 2));
                    alvResult.add(alv.get(c * 3));
                    alvResult.add(alv.get(c * 3 + 1));
                    alvResult.add(alv.get(c * 3 + 2));

                    alvResult.add(alv.get(a * 3));
                    alvResult.add(alv.get(a * 3 + 1));
                    alvResult.add(alv.get(a * 3 + 2));
                    alvResult.add(alv.get(c * 3));
                    alvResult.add(alv.get(c * 3 + 1));
                    alvResult.add(alv.get(c * 3 + 2));
                    alvResult.add(alv.get(d * 3));
                    alvResult.add(alv.get(d * 3 + 1));
                    alvResult.add(alv.get(d * 3 + 2));

                    for (int i = 0; i < 3; i++) {
                        ab[i] = alv.get(a * 3 + i) - alv.get(b * 3 + i);
                        bc[i] = alv.get(b * 3 + i) - alv.get(c * 3 + i);
                    }
                    norl[0] = ab[1] * bc[2] - ab[2] * bc[1];
                    norl[1] = ab[2] * bc[0] - ab[0] * bc[2];
                    norl[2] = ab[0] * bc[1] - ab[1] * bc[0];
                    for (int i = 0; i < 6; i++) {
                        norlArr.add(norl[0]);
                        norlArr.add(norl[1]);
                        norlArr.add(norl[2]);
                    }
                }
            }

            ObjModel objModel = new ObjModel();
            int size = alvResult.size();
            FloatBuffer posVertices = ByteBuffer.allocateDirect(size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            for (int i = 0; i < size; i++) {
                posVertices.put(alvResult.get(i));
            }
            posVertices.position(0);
            int posVerticesCount = size / 3;
            objModel.setPosVertices(posVertices, posVerticesCount);

            size = norlArr.size();
            FloatBuffer norVertices = ByteBuffer.allocateDirect(size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            for (int i = 0; i < size; i++) {
                norVertices.put(norlArr.get(i));
            }
            norVertices.position(0);
            objModel.setNorVertices(norVertices);
            return objModel;
        }
    }

    public static ObjModel read(Context context, String file) throws IOException {
        ArrayList<ObjModel> data = new ArrayList<>();
        ArrayList<Float> oVs = new ArrayList<>();//原始顶点坐标列表
        ArrayList<Float> oVNs = new ArrayList<>();    //原始顶点法线列表
        ArrayList<Float> oVTs = new ArrayList<>();    //原始贴图坐标列表
        ArrayList<Float> oFVs = new ArrayList<>();     //面顶点
        ArrayList<Float> oFVNs = new ArrayList<>();
        ArrayList<Float> oFVTs = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open(file);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String temps;
//            while ((temps = br.readLine()) != null) {
//                if ("".equals(temps)) {
//
//                } else {
//                    String[] tempsa = temps.split("[ ]+");
//                    switch (tempsa[0].trim()) {
//                        case "mtllib":  //材质
//
//                            InputStream stream = context.getAssets().open(parent + tempsa[1]);
//                            mTls = readMtl(stream);
//                            break;
//                        case "usemtl":  //采用纹理
//                            if (mTls != null) {
//                                nowMtl = mTls.get(tempsa[1]);
//                            }
//                            if (mObjs.containsKey(tempsa[1])) {
//                                nowObj = mObjs.get(tempsa[1]);
//                            } else {
//                                nowObj = new ObjModel();
//                                nowObj.mtl = nowMtl;
//                                mObjs.put(tempsa[1], nowObj);
//                            }
//                            break;
//                        case "v":       //原始顶点
//                            read(tempsa, oVs);
//                            break;
//                        case "vn":      //原始顶点法线
//                            read(tempsa, oVNs);
//                            break;
//                        case "vt":
//                            read(tempsa, oVTs);
//                            break;
//                        case "f":
//                            for (int i = 1; i < tempsa.length; i++) {
//                                String[] fs = tempsa[i].split("/");
//                                int index;
//                                if (fs.length > 0) {
//                                    //顶点索引
//                                    index = Integer.parseInt(fs[0]) - 1;
//                                    nowObj.addVert(oVs.get(index * 3));
//                                    nowObj.addVert(oVs.get(index * 3 + 1));
//                                    nowObj.addVert(oVs.get(index * 3 + 2));
//                                }
//                                if (fs.length > 1) {
//                                    //贴图
//                                    index = Integer.parseInt(fs[1]) - 1;
//                                    nowObj.addVertTexture(oVTs.get(index * 2));
//                                    nowObj.addVertTexture(oVTs.get(index * 2 + 1));
//                                }
//                                if (fs.length > 2) {
//                                    //法线索引
//                                    index = Integer.parseInt(fs[2]) - 1;
//                                    nowObj.addVertNorl(oVNs.get(index * 3));
//                                    nowObj.addVertNorl(oVNs.get(index * 3 + 1));
//                                    nowObj.addVertNorl(oVNs.get(index * 3 + 2));
//                                }
//                            }
//                            break;
//                    }
//                }
//            }
        } finally {

        }


        return null;
    }

    public Buffer getPosVertices() {
        return mPosVertices;
    }

    public Buffer getNorVertices() {
        return mNorVertices;
    }

    public Buffer getTexVertices() {
        return mTexVertices;
    }

    public int getPosVertexCount() {
        return mPosVertexCount;
    }

    private void setPosVertices(Buffer posVertices, int posVerticesCount) {
        this.mPosVertices = posVertices;
        this.mPosVertexCount = posVerticesCount;
    }

    private void setNorVertices(Buffer norVertices) {
        this.mNorVertices = norVertices;
    }


    public static class MtlModel {

        public String newmtl;
        public float[] Ka = new float[3];     //阴影色
        public float[] Kd = new float[3];     //固有色
        public float[] Ks = new float[3];     //高光色
        public float[] Ke = new float[3];     //
        public float Ns;                    //shininess
        public String map_Kd;               //固有纹理贴图
        public String map_Ks;               //高光纹理贴图
        public String map_Ka;               //阴影纹理贴图

        public int illum;

    }


}
