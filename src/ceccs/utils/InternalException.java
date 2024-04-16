package ceccs.utils;

public class InternalException extends Exception {

    public InternalException(String info) {
        super(info);
    }

    public static double checkSafeDivision(double numerator, double denominator) throws InternalException {
        double value = numerator / denominator;

        if (Double.isInfinite(value)) {
            throw new InternalException(String.format(
                    "division does not produce finite number: %f / %f\n",
                    numerator, denominator
            ));
        } else if (Double.isNaN(value)) {
            throw new InternalException(String.format(
                    "division produces NaN: %f / %f\n",
                    numerator, denominator
            ));
        }

        return value;
    }

}
