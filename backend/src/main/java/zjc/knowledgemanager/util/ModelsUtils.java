package zjc.knowledgemanager.util;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import zjc.knowledgemanager.entity.dashscope.DashScopeModel;
import zjc.knowledgemanager.entity.dashscope.DashScopeModels;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public final class ModelsUtils {

    private final static String MODELS_FILE_PATH = "models.yaml";

    private static final String MODEL = "model";

    private static final String DESC = "desc";

    private ModelsUtils() {
    }

    public static Set<Map<String, String>> getDashScopeModels() throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream resourceAsStream = ModelsUtils.class.getClassLoader().getResourceAsStream(MODELS_FILE_PATH);
        DashScopeModels models = mapper.readValue(resourceAsStream, DashScopeModels.class);

        Set<Map<String, String>> resultSet = new HashSet<>();
        for (DashScopeModel model : models.getDashScope()) {
            Map<String, String> modelMap = new HashMap<>();
            modelMap.put(MODEL, model.getName());
            modelMap.put(DESC, model.getDescription());
            resultSet.add(modelMap);
        }

        return resultSet;
    }

}

