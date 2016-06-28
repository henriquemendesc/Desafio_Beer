package malalai.prova_desafio.persistence;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Bob on 25/06/2016.
 * Aqui deve ser implementado tdo o controle do banco de dados
 * Ou seja, inserts, updates, consultas
 */
public class LocalController {
    private LocalModel localModel;
    LocalListener localListener;

    public LocalController(Context context, LocalListener localListener){
        localModel = new LocalModel(context);
        this.localListener = localListener;
    }

    public void addLocal(String nome, String endereco, String bebida, double latitude, double longitude) {
        //Monta o JSON com base nas informações passadas como parâmetro
        JSONObject local = new JSONObject();
        try {
            local.put("name", nome);
            local.put("address", endereco);
            switch (bebida.toString()) {
                case "Cerveja": local.put("beverage", 1);
                    break;
                case "Café": local.put("beverage", 2);
                    break;
                case "Ambos": local.put("beverage", 3);
                    break;
            }
            local.put("latitude", latitude);
            local.put("longitude", longitude);

        } catch (JSONException e) {
            localListener.onFailure(e);
            return;
        }
        //Chama o Model para adicionar o local na WEB API
        localModel.addLocal(local, localListener);
    }
}
