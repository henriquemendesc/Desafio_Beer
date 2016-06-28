package malalai.prova_desafio.persistence;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Bob on 25/06/2016.
 * Aqui deve ser implementado toda a criação da base de dados
 */
public class LocalModel {
    private static final String TAG = "Desafio";
    private static String url = "https://c7q5vyiew7.execute-api.us-east-1.amazonaws.com/prod/places";
    private RequestQueue queue;

    public LocalModel (Context context){
        queue = Volley.newRequestQueue(context);
    }

    public void addLocal(JSONObject local, final LocalListener localListener){

        //Cria a requisição
        JsonObjectRequest req = new JsonObjectRequest(url, local,
                //Método que será chamado caso a inclusão dê certo
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        LocalResponse localResponse = new LocalResponse();
                        try {
                            //Monta a resposta com base no JSON retornado pela web api
                            localResponse.message = response.getString("message");
                            localResponse.place_id = response.getString("place_id");
                        } catch (JSONException e) {
                            //Caso dê algum erro ao montar a resposta, informa à view que houve erro
                            localListener.onFailure(e);
                            return;
                        }
                        //Informa a view que a inclusão deu certo e envia os dados da resposta
                        localListener.onSuccess(localResponse);
                    }
                },
                //Método que será chamado caso dê algum erro na inclusão
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Caso dê algum erro ao incluir o local, informa à view que houve erro
                        localListener.onFailure(error);
                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                //inclui os cabeçalhos específicos da API
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", "IfXJnQVdjo1fI4z6OQTWB6RPJ8Qs4JbcaDOZ83vt");
                return headers;
            }
        };

        queue.add(req);
    }
}
