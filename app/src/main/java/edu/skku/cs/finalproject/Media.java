package edu.skku.cs.finalproject;

import static edu.skku.cs.finalproject.ViewModel.mStartPlaying;
import static edu.skku.cs.finalproject.ViewModel.mStartRecording;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Media {

    int audioSource = MediaRecorder.AudioSource.MIC;
    int sampleRateInHz = 44100;
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

    public Thread mRecordThread = null;
    public Thread mPlayThread = null;
    FileOutputStream fos = null;

    private String recordingFile;
    public static String base64String = null;

    AudioRecord recorder = null;
    AudioTrack mAudioTrack = null;
    MediaPlayer player = null;

    public Media(String recordingFile){
        this.recordingFile = recordingFile;
    }

    @SuppressLint("MissingPermission")
    public void startRecording() {

        recorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        recorder.startRecording();

        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] readData = new byte[bufferSizeInBytes];

                try {
                    fos = new FileOutputStream(recordingFile);
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }

                while(mStartRecording) {
                    int ret = recorder.read(readData, 0, bufferSizeInBytes);  //  AudioRecord??? read ????????? ?????? pcm data ??? ?????????
                    try {
                        fos.write(readData, 0, bufferSizeInBytes);    //  ????????? readData ??? ????????? write ???
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        mRecordThread.start();

    }

    public void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        base64String=getBase64(recordingFile);
    }

    public void startPlaying(ViewModel viewModel) {
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        mPlayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] writeData = new byte[bufferSizeInBytes];
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(recordingFile);
                }catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                DataInputStream dis = new DataInputStream(fis);
                mAudioTrack.play();  // write ?????? ?????? play ??? ?????? ????????? ????????? ???

                while(mStartPlaying) {
                    try {
                        int ret = dis.read(writeData, 0, bufferSizeInBytes);
                        if (ret <= 0) {
                            viewModel.onPlay();
                            break;
                        }
                        mAudioTrack.write(writeData, 0, ret); // AudioTrack ??? write ??? ?????? ???????????? ?????????
                    }catch (IOException e) {
                        e.printStackTrace();
                    }

                }


                try {
                    dis.close();
                    fis.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayThread.start();

    }


    public void stopPlaying() {
        if(mAudioTrack!=null){
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }



    private String getBase64(String filename) {
        byte[] bytes = new byte[0];
        try{
            bytes = Files.readAllBytes(Paths.get(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
