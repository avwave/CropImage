package com.twozelex.cropimage;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecodingInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ant on 15/3/7.
 */
public class ZLXImageDecoder extends BaseImageDecoder {
    /**
     * @param loggingEnabled Whether debug logs will be written to LogCat. Usually should match {@link
     *                       com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder#writeDebugLogs()
     *                       ImageLoaderConfiguration.writeDebugLogs()}
     */
    public ZLXImageDecoder(boolean loggingEnabled) {
        super(loggingEnabled);
    }

    // http://stackoverflow.com/questions/16909591/universal-image-loader-get-original-image-size
    public static Map<String, PhotoFileInfo> urlToSizeMap = new ConcurrentHashMap<String, PhotoFileInfo>();

    @Override
    protected ImageFileInfo defineImageSizeAndRotation(InputStream imageStream, ImageDecodingInfo decodingInfo) throws IOException {
        ImageFileInfo info = super.defineImageSizeAndRotation(imageStream, decodingInfo);
        urlToSizeMap.put(decodingInfo.getOriginalImageUri(), new PhotoFileInfo(info.imageSize, info.exif));
        return info;
    }

    public class PhotoFileInfo extends ImageFileInfo {
        protected PhotoFileInfo(ImageSize imageSize, ExifInfo exif) {
            super(imageSize, exif);
        }

        public  int getRotation() {
            return this.exif.rotation;
        }
    }
}

