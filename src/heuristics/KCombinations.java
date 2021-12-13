package heuristics;

import java.util.*;
	
public class KCombinations {
    public static ArrayList<int[]> enumKCombos(int[] array, int k) {
        ArrayList<int[]> comboList = new ArrayList<int[]>();
        
        assert(k > 0);

        if (k > 1) {

            assert(array.length >= k);

            // Store the first member of the array.
            int[] first = new int[1];
            first[0] = array[0];
            array = Arrays.copyOfRange(array, 1, array.length);

            while (array.length + 1 >= k) {
                ArrayList<int[]> subComboList = new ArrayList<int[]>();
                // Call the recursive function and temporarily store the
                //   returned arrays.
                subComboList = enumKCombos(array, k - 1);

                // Concatenate the stored first member onto the front of the
                //   returned arrays.
                int[] subArray;
                for (int i = 0; i < subComboList.size(); i++) {
                    subArray = subComboList.get(i);
                    int[] concatenated = new int[subArray.length + 1];
                    concatenated[0] = first[0];
                    for (int j = 0; j < subArray.length; j++) {
                        concatenated[j + 1] = subArray[j];
                    }
                    comboList.add(concatenated);
                }

                // Add the newly-concatenated arrays to the comboList.
                // Replace first with array[0].
                first[0] = array[0];

                // Splice array to remove the first member.
                array = Arrays.copyOfRange(array, 1, array.length);
            }
        } else {
            // Return the individual members of array as individual 1-member
            //   arrays.
            for (int i = 0; i < array.length; i++) {
                comboList.add(Arrays.copyOfRange(array, i, i + 1));
            }
        } 

        return comboList;
    }

    
    public static String printArray(int[] array) {
        String arrayStr = "";
        for (int i = 0; i < array.length; i++)
            arrayStr += array[i] + " ";
        return arrayStr;
    }
}

