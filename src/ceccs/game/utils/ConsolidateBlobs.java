package ceccs.game.utils;

import ceccs.game.objects.elements.Blob;

import java.util.ArrayList;

public class ConsolidateBlobs {

    public static ArrayList<Blob> convert(ArrayList<ArrayList<Blob>> blobs) {
        ArrayList<Blob> output = new ArrayList<>();

        for (ArrayList<Blob> blobList : blobs) {
            output.addAll(blobList);
        }

        return output;
    }

}
