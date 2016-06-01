/*
 * 
 */
package com.wepay.android.internal.CardReader.DeviceHelpers;

import com.roam.roamreaderunifiedapi.constants.Parameter;

import java.util.Iterator;
import java.util.Map;

/**
 * The Class RoamHelper.
 */
public class RoamHelper {

    /**
     * Gets the first name.
     *
     * @param map the map
     * @return the first name
     */
    public static String getFirstName(Map<Parameter, Object> map) {
        return getNamePart(1, map);
    }

    /**
     * Gets the last name.
     *
     * @param map the map
     * @return the last name
     */
    public static String getLastName(Map<Parameter, Object> map) {
        return getNamePart(0, map);
    }

    /**
     * Gets the full name
     *
     * @param map the map
     * @return full name if available, otherwise null
     */
    public static String getFullName(Map<Parameter, Object> map) {
        String firstName = RoamHelper.getFirstName(map) != null ? RoamHelper.getFirstName(map) : "";
        String lastName = RoamHelper.getLastName(map) != null ? RoamHelper.getLastName(map) : "";

        String name = firstName + " " + lastName;
        name = name.trim();

        return name;
    }

    /**
     * Gets the name part.
     *
     * @param index the index
     * @param map the map
     * @return the name part if available, otherwise null
     */
    private static String getNamePart(int index, Map<Parameter, Object> map) {
        String result = null;

        String encName = map.get(Parameter.CardHolderName).toString();
        encName = encName.trim();
        String[] parts = encName.split("/");

        if (parts.length > index) {
            result = parts[index];
        }

        if (result == null || result.trim().equalsIgnoreCase("")) {
            result = "Unknown";
        }

        return result;
    }

    /**
     * Gets the payment description.
     *
     * @param map the map
     * @return the payment description
     */
    public static String getPaymentDescription(Map<Parameter, Object> map) {
        String result = "";

        if (map.containsKey(Parameter.PAN)) {
            result = map.get(Parameter.PAN).toString();
            if (result == null) {
                result = "";
            }
        }

        return result;
    }

    /**
     * Gets the formatted response string.
     *
     * @param map the map
     * @return the formatted response string
     */
    public static String getFormattedResponseString(Map<Parameter, Object> map) {
        StringBuilder sb = new StringBuilder();
        Iterator<Parameter> iterator = map.keySet().iterator();
        sb.append("{");
        Parameter curr;
        while (iterator.hasNext()) {
            sb.append("\n");
            curr = iterator.next();
            sb.append(curr.toString() + ":" + map.get(curr).toString());
        }
        sb.append("\n}");
        return sb.toString();
    }
}
