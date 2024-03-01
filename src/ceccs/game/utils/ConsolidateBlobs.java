package ceccs.game.utils;

import ceccs.game.objects.elements.Blob;

import java.util.*;

public class ConsolidateBlobs {

    @SafeVarargs
    public static ArrayList<Blob> convert(AbstractMap<UUID, ? extends Blob>... blobs) {
        ArrayList<Blob> output = new ArrayList<>();

        for (AbstractMap<UUID, ?> blobBlob : blobs) {
            output.addAll((Collection<? extends Blob>) blobBlob.values());
        }

        return output;
    }

}
