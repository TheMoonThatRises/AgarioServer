package ceccs.game.utils;

import ceccs.game.objects.Camera;
import ceccs.game.objects.elements.Blob;
import ceccs.network.utils.CustomID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ConsolidateBlobs {

    @SafeVarargs
    public static ArrayList<Blob> convert(Camera camera, Map<CustomID, ? extends Blob>... blobs) {
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
