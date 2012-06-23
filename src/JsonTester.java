import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class JsonTester {

	public static void main(String[] args) {
		Gson gson = new GsonBuilder().create();

		JsonObject object = new JsonObject();
		object.addProperty("method", "trigger");
		String string = gson.toJson(object);
		System.out.println(string);
	}

}
