package ceccs.game.utils;

import ceccs.game.objects.Camera;
import ceccs.game.objects.elements.Blob;
import ceccs.network.utils.CustomID;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;

public class ConsolidateBlobs {

    @SafeVarargs
    public static ArrayList<Blob> convert(Camera camera, AbstractMap<CustomID, ? extends Blob>... blobs) {
        ArrayList<Blob> output = new ArrayList<>();

        Arrays.stream(blobs)
                .parallel()
                .forEach(blobBlob -> output.addAll(
                        blobBlob.values()
                                .stream()
                                .parallel()
                                .filter(blob -> blob.visibilityCulling(camera))
                                .toList()
                ));

        return output;
    }

}
