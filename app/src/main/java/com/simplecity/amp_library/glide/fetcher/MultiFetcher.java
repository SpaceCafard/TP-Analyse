package com.simplecity.amp_library.glide.fetcher;

import android.content.Context;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.UserSelectedArtwork;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import java.io.File;
import java.io.InputStream;

public class MultiFetcher implements DataFetcher<InputStream> {

    private static final String TAG = "MultiFetcher";

    private Context applicationContext;

    private DataFetcher<InputStream> dataFetcher;

    private ArtworkProvider artworkProvider;

    private SettingsManager settingsManager;

    private boolean allowOfflineDownload = false;

    public MultiFetcher(Context context, ArtworkProvider artworkProvider, SettingsManager settingsManager, boolean allowOfflineDownload) {
        applicationContext = context;
        this.artworkProvider = artworkProvider;
        this.settingsManager = settingsManager;
        this.allowOfflineDownload = allowOfflineDownload;
    }

    private InputStream loadData(DataFetcher<InputStream> dataFetcher, Priority priority) {
        InputStream inputStream;
        try {
            inputStream = dataFetcher.loadData(priority);
        } catch (Exception e) {
            if (dataFetcher != null) {
                dataFetcher.cleanup();
            }
            inputStream = null;
        }
        return inputStream;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        InputStream inputStream = tryLoadUserSelectedArtwork(priority);

        if (inputStream == null) {
            inputStream = tryLoadFromMediaStore(priority);
        }

        if (inputStream == null) {
            inputStream = tryLoadFromLocalSources(priority);
        }

        if (inputStream == null) {
            inputStream = tryLoadFromRemote(priority);
        }

        return inputStream;
    }

    private InputStream tryLoadUserSelectedArtwork(Priority priority) {
        UserSelectedArtwork userSelectedArtwork = ((ShuttleApplication) applicationContext)
                .userSelectedArtwork.get(artworkProvider.getArtworkKey());
        if (userSelectedArtwork != null) {
            switch (userSelectedArtwork.type) {
                case ArtworkProvider.Type.MEDIA_STORE:
                    dataFetcher = new MediaStoreFetcher(applicationContext, artworkProvider);
                    break;
                case ArtworkProvider.Type.FOLDER:
                    dataFetcher = new FolderFetcher(artworkProvider, new File(userSelectedArtwork.path));
                    break;
                case ArtworkProvider.Type.TAG:
                    dataFetcher = new TagFetcher(artworkProvider);
                    break;
                case ArtworkProvider.Type.REMOTE:
                    dataFetcher = new RemoteFetcher(artworkProvider);
                    break;
                default:
                    return null;
            }
            return loadData(dataFetcher, priority);
        }
        return null;
    }

    private InputStream tryLoadFromMediaStore(Priority priority) {
        if (!settingsManager.ignoreMediaStoreArtwork()) {
            dataFetcher = new MediaStoreFetcher(applicationContext, artworkProvider);
            return loadData(dataFetcher, priority);
        }
        return null;
    }

    private InputStream tryLoadFromLocalSources(Priority priority) {
        InputStream inputStream = null;
        if (settingsManager.preferEmbeddedArtwork()) {
            if (!settingsManager.ignoreEmbeddedArtwork()) {
                dataFetcher = new TagFetcher(artworkProvider);
                inputStream = loadData(dataFetcher, priority);
            }
            if (inputStream == null && !settingsManager.ignoreFolderArtwork()) {
                dataFetcher = new FolderFetcher(artworkProvider, null);
                inputStream = loadData(dataFetcher, priority);
            }
        } else {
            if (!settingsManager.ignoreFolderArtwork()) {
                dataFetcher = new FolderFetcher(artworkProvider, null);
                inputStream = loadData(dataFetcher, priority);
            }
            if (inputStream == null && !settingsManager.ignoreEmbeddedArtwork()) {
                dataFetcher = new TagFetcher(artworkProvider);
                inputStream = loadData(dataFetcher, priority);
            }
        }
        return inputStream;
    }

    private InputStream tryLoadFromRemote(Priority priority) {
        if (allowOfflineDownload
                || (settingsManager.canDownloadArtworkAutomatically()
                && ShuttleUtils.isOnline(applicationContext, true))) {
            dataFetcher = new RemoteFetcher(artworkProvider);
            return loadData(dataFetcher, priority);
        }
        return null;
    }

    @Override
    public void cleanup() {
        if (dataFetcher != null) {
            dataFetcher.cleanup();
        }
    }

    @Override
    public void cancel() {
        if (dataFetcher != null) {
            dataFetcher.cancel();
        }
    }

    private String getCustomArtworkSuffix(Context context) {
        if (((ShuttleApplication) context.getApplicationContext()).userSelectedArtwork.containsKey(artworkProvider.getArtworkKey())) {
            UserSelectedArtwork userSelectedArtwork = ((ShuttleApplication) context.getApplicationContext()).userSelectedArtwork.get(artworkProvider.getArtworkKey());
            return "_" + userSelectedArtwork.type + "_" + (userSelectedArtwork.path == null ? "" : userSelectedArtwork.path.hashCode());
        }
        return "";
    }

    @Override
    public String getId() {
        return artworkProvider.getArtworkKey() + getCustomArtworkSuffix(applicationContext);
    }
}
