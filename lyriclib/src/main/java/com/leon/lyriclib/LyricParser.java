package com.leon.lyriclib;

import android.support.annotation.NonNull;
import android.util.Log;

import com.itheima.mvplayer.model.LyricBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LyricParser {
    public static final String TAG = "LyricParser";

    private ExecutorService mExecutorService = Executors.newCachedThreadPool();

    private OnLyricChangeListener mOnLyricChangeListener;

    private static LyricParser sLyricParser;

    private LyricParser() {
    }

    public static LyricParser getInstance() {
        if (sLyricParser == null) {
            synchronized (LyricParser.class) {
                if (sLyricParser == null) {
                    sLyricParser = new LyricParser();
                }
            }
        }
        return sLyricParser;
    }


    public void parseLyric(@NonNull String path, @NonNull OnLyricChangeListener listener) {
        mOnLyricChangeListener = listener;
        File file = new File(path);
        if (file.exists()) {
            mExecutorService.execute(new LoadLyricTask(file));
        } else {
            file = new File(path.replace(".lrc", ".txt"));
            if (file.exists()) {
                mExecutorService.execute(new LoadLyricTask(file));
            } else {
                mOnLyricChangeListener.onLyricNotFound();
            }
        }
    }

    public interface OnLyricChangeListener {

        void onLyricNotFound();

        void onLyricLoaded(List<LyricBean> lyrics);
    }


    private class LoadLyricTask implements Runnable {

        private File mFile;

        public LoadLyricTask(File file) {
            mFile = file;
        }

        @Override
        public void run() {
            List<LyricBean> lyricBeanList = new ArrayList<LyricBean>();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile), "GBK"));
                String readLine = reader.readLine();
                while (readLine != null) {
                    //解析一行歌词
                    List<LyricBean> lyrics = parseLyricLine(readLine);
                    lyricBeanList.addAll(lyrics);
                    readLine = reader.readLine();
                }
                //歌词按照时间戳进行排序
                Collections.sort(lyricBeanList, new Comparator<LyricBean>() {
                    @Override
                    public int compare(LyricBean o1, LyricBean o2) {
                        return o1.getTimestamp() - o2.getTimestamp();//升序排列
                    }
                });

                mOnLyricChangeListener.onLyricLoaded(lyricBeanList);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 解析一行文本，转换成歌词的bean集合
     *
     * @param readLine 读取的一行文本
     * @return 解析的结果
     */
    private static List<LyricBean> parseLyricLine(String readLine) {
        List<LyricBean> lyrics = new ArrayList<LyricBean>();
        //[01:22.04][02:35.04]寂寞的夜和谁说话
        String[] arrays = readLine.split("]");
        //[01:22.04    [02:35.04    寂寞的夜和谁说话
        for (int i = 0; i < arrays.length - 1; i++) {
            LyricBean lyricBean = new LyricBean();
            lyricBean.setTimestamp(parseTimeStamp(arrays[i]));
            lyricBean.setLyric(arrays[arrays.length - 1]);
            lyrics.add(lyricBean);
        }
        return lyrics;
    }

    /**
     *  解析歌词的时间戳
     */
    private static int parseTimeStamp(String time) {
        Log.d(TAG, "parseTimeStamp: " + time);
        //[01:22.04
        String[] array1 = time.split(":");
        //[01  22.04
        String minute = array1[0].substring(1);//01
        //22.04
        String[] array2 = array1[1].split("\\.");
        // 22 04
        String second  = array2[0];
        String millis = array2[1];

        return Integer.parseInt(minute) * 60 * 1000 + Integer.parseInt(second) * 1000 + Integer.parseInt(millis);
    }

}
