package com.faceauth.faceauth.service;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Base64;

@Service
public class FaceService {

    private final CascadeClassifier faceDetector;

    public FaceService() throws Exception {
        nu.pattern.OpenCV.loadLocally();
        InputStream is = getClass().getResourceAsStream(
                "/haarcascade_frontalface_default.xml");
        File tmpFile = File.createTempFile("haarcascade", ".xml");
        tmpFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            is.transferTo(out);
        }
        faceDetector = new CascadeClassifier(tmpFile.getAbsolutePath());
    }

    public Mat base64ToMat(String base64) {
        byte[] imgBytes = Base64.getDecoder().decode(base64);
        MatOfByte mob = new MatOfByte(imgBytes);
        return org.opencv.imgcodecs.Imgcodecs.imdecode(mob,
                org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR);
    }

    public Mat detectFace(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 5,
                0, new Size(80, 80), new Size());

        Rect[] detected = faces.toArray();
        if (detected.length == 0) return null;

        Rect largest = detected[0];
        for (Rect r : detected) {
            if (r.width * r.height > largest.width * largest.height)
                largest = r;
        }

        Mat face = new Mat(gray, largest);
        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(100, 100));
        return resized;
    }

    public float[] extractEmbedding(Mat faceMat) {
        int gridSize = 8, bins = 16;
        float[] descriptor = new float[gridSize * gridSize * bins];
        int cellW = faceMat.cols() / gridSize;
        int cellH = faceMat.rows() / gridSize;
        int idx = 0;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                Mat patch = new Mat(faceMat,
                        new Rect(c * cellW, r * cellH, cellW, cellH));
                float[] hist = lbpHistogram(patch, bins);
                System.arraycopy(hist, 0, descriptor, idx, bins);
                idx += bins;
            }
        }
        return descriptor;
    }

    private float[] lbpHistogram(Mat patch, int bins) {
        int[] hist = new int[bins];
        for (int r = 1; r < patch.rows() - 1; r++) {
            for (int c = 1; c < patch.cols() - 1; c++) {
                int center = (int) patch.get(r, c)[0];
                int code = 0;
                int[][] nb = {{-1,-1},{-1,0},{-1,1},{0,1},
                              {1,1},{1,0},{1,-1},{0,-1}};
                for (int i = 0; i < 8; i++) {
                    int n = (int) patch.get(
                            r + nb[i][0], c + nb[i][1])[0];
                    if (n >= center) code |= (1 << i);
                }
                hist[code * bins / 256]++;
            }
        }
        float total = (patch.rows() - 2f) * (patch.cols() - 2f);
        float[] norm = new float[bins];
        for (int i = 0; i < bins; i++) norm[i] = hist[i] / total;
        return norm;
    }

    public double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public byte[] serializeEmbedding(float[] emb) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(emb.length);
        for (float f : emb) dos.writeFloat(f);
        return bos.toByteArray();
    }

    public float[] deserializeEmbedding(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(data));
        int len = dis.readInt();
        float[] emb = new float[len];
        for (int i = 0; i < len; i++) emb[i] = dis.readFloat();
        return emb;
    }
}