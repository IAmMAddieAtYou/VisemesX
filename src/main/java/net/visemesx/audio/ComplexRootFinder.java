package net.visemesx.audio;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

public class ComplexRootFinder {
    public static void main(String[] args) {
        // Coefficients of polynomial (e.g., x^3 - 6x^2 + 11x - 6 = (x-1)(x-2)(x-3))
        double[] coefficients = {-6.0, 11.0, -6.0, 1.0};


        // Output all roots
    }

    public static Complex[] findRoots(double[] lpcCoefficients) {
        // System.out.println("[VisemeDetector] TEST2");
        if (lpcCoefficients == null || lpcCoefficients.length < 2) {
            return null; // Not a valid polynomial
        }
        // System.out.println("[VisemeDetector] TEST2");
        // Check for NaN or Infinity in coefficients
        for (double coef : lpcCoefficients) {

            if (Double.isNaN(coef) || Double.isInfinite(coef)) {
                return null;
            }
        }
        // System.out.println("[VisemeDetector] TEST2");
        // Normalize coefficients if leading term is zero
        int firstNonZero = -1;
        for (int i = 0; i < lpcCoefficients.length; i++) {
            if (lpcCoefficients[i] != 0.0) {
                firstNonZero = i;
                break;
            }
        }
        // System.out.println("[VisemeDetector] TEST2");
        if (firstNonZero == -1) {
            return null; // All zero polynomial
        }
        //   System.out.println("[VisemeDetector] TEST2");
        double[] trimmedCoeffs = new double[lpcCoefficients.length - firstNonZero];
        System.arraycopy(lpcCoefficients, firstNonZero, trimmedCoeffs, 0, trimmedCoeffs.length);
        // System.out.println("[VisemeDetector] TEST2");

        try {
            //System.out.println("[VisemeDetector] TEST2");
        LaguerreSolver solver = new LaguerreSolver();
            //     System.out.println("[VisemeDetector] TEST2");
        // Find all roots (real and complex)
         // 0.0 is the starting value
            //     System.out.println("[VisemeDetector] TEST2");
        return solver.solveAllComplex(trimmedCoeffs, 0.0,100);
        } catch (NullArgumentException | NoDataException | TooManyEvaluationsException e) {
            return null;
        }
    }
}
