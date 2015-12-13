package svsync;

import java.util.Map;

/**
 * @author Sval
 */
public class TargetInfo extends Info {

    private TargetInfo(Map<String, String> params) {
        super(params);
    }
    
    public static TargetInfo createTarget(Map<String, String> params) {
        return new TargetInfo(params);
    }
}
