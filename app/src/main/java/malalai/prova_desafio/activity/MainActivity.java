package malalai.prova_desafio.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.io.IOException;
import java.util.List;

import malalai.prova_desafio.R;
import malalai.prova_desafio.adapter.PlaceAutocompleteAdapter;
import malalai.prova_desafio.persistence.LocalController;
import malalai.prova_desafio.persistence.LocalListener;
import malalai.prova_desafio.persistence.LocalResponse;
import malalai.prova_desafio.utils.Permissions;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener, LocalListener {
    private static final String TAG = "Desafio";
    private String[] bebidas = new String[]{"Cerveja", "Café", "Ambos"};
    protected GoogleApiClient mGoogleApiClient;
    private ProgressDialog dialog;
    private LocalController localController;
    PlaceAutocompleteAdapter adapter;
    //Editores
    private EditText edtNome;
    private EditText edtLongitude;
    private EditText edtLatitude;
    private AutoCompleteTextView edtEndereco;
    private EditText edtNumero;
    private EditText edtUF;
    private EditText edtPais;
    private EditText edtBairro;
    private EditText edtCidade;
    private Spinner spnBebidas;

    //==================Métodos sobrescritos da AppCompatActivity======================//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Solicita as permissões
        String[] permissoes = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
        };
        Permissions.validate(this, 0, permissoes);

        //Criando objeto para conexão com google play services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();

        //Relacionando editores com variáveis
        edtNome = (EditText) findViewById(R.id.edt_Nome_Local);
        edtLongitude = (EditText) findViewById(R.id.edt_Long_Local);
        edtLatitude = (EditText) findViewById(R.id.edt_Lati_Local);

        // Buscando o AutoCompleteTextView
        edtEndereco = (AutoCompleteTextView) findViewById(R.id.edt_Endereco_Local);

        // Registrando qual método será chamado ao executar o evento de click
        edtEndereco.setOnItemClickListener(edtEnderecoClickListener);

        // Setando o adapter utilizado pelo componente AutoCompleteTextView
        adapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, null, null);
        edtEndereco.setAdapter(adapter);

        //Montando opções do spinner
        spnBebidas = (Spinner) findViewById(R.id.spn_Bebida_Local);
        if (spnBebidas != null) {
            ArrayAdapter<String> adpBebidas = new ArrayAdapter<String>(this, R.layout.spinner_selected_item, bebidas);
            adpBebidas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnBebidas.setAdapter(adpBebidas);
        }

        //Inicializando controller
        localController = new LocalController(this, this);

        //Setando método do botão salvar
        Button btnSalvar = (Button) findViewById(R.id.btn_Salvar_Local);
        btnSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!campoObrigatorio()) {
                    localController.addLocal(edtNome.getText().toString(),
                            edtEndereco.getText().toString(),
                            spnBebidas.getSelectedItem().toString(),
                            Double.valueOf(edtLatitude.getText().toString()),
                            Double.valueOf(edtLongitude.getText().toString()));
                    showProgress(true);
                }
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Conecta no Google Play Services
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        // Desconecta do Google Play Services
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    //==================Métodos de controle do Google Play Services (GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks) ======================//
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        showAlertOk("Não foi possível conectar na Google API Client: Error " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "Conectado no Google Play Services!");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e(TAG, "Conexão interrompida.");
    }

    //==================Métodos de controle do LocationListener======================//
    @Override
    public void onLocationChanged(Location location) {
        //
        if (location != null) {
            try {
                Address endereco = buscarEndereco(location.getLatitude(), location.getLongitude());
                setEditText(edtLongitude, String.valueOf(location.getLongitude()));
                setEditText(edtLatitude, String.valueOf(location.getLatitude()));

                setEditText(edtEndereco, endereco.getThoroughfare() + ", " + //Rua
                        endereco.getSubThoroughfare() + " - " + //Número
                        endereco.getSubLocality() + ", " + //Bairro
                        endereco.getLocality() + ", " + //Cidade
                        endereco.getAdminArea() + " - " + //Estado
                        endereco.getCountryName()); //País

            } catch (IOException e) {
                Log.e(TAG, "Erro ao buscar endereço: " + e.getMessage());
            }
        }
        stopLocationUpdates();
    }

    //==================Métodos de controle do LocalListener======================//
    @Override
    public void onSuccess(LocalResponse localResponse) {
        //Local adicionado com sucesso
        showProgress(false);
        showAlertOk(localResponse.message + "\nLocal ID: " + localResponse.place_id);
        limparCampos();
    }

    @Override
    public void onFailure(Throwable t) {
        //Erro ao adicionar local
        showProgress(false);
        showAlertOk("Erro ao tentar adicionar local. Tente novamente mais tarde.\n\nErro: " + t.getMessage());
    }

    //==================Métodos para autocomplete endreço================//
    private AdapterView.OnItemClickListener edtEnderecoClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = adapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Log.i(TAG, "Autocomplete item selecionado: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Log.i(TAG, "Chamado getPlaceById paga buscar detalhes do local com ID = " + placeId);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            Place place = places.get(0);

            setEditText(edtLongitude, String.valueOf(place.getLatLng().longitude));
            setEditText(edtLatitude, String.valueOf(place.getLatLng().latitude));

            Log.i(TAG, "Place details received: " + place.getName());
            places.release();
        }
    };

    //==================Métodos auxiliares======================//
    private void showAlertOk(String mensagem) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Alerta");
        alertDialog.setMessage(mensagem);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void setEditText(EditText edt, String text) {
        if (edt != null)
            edt.setText(text);
    }

    private Context getContext() {
        return this;
    }

    protected void showProgress(boolean show) {
        if (show) {
            dialog = ProgressDialog.show(this, "Carregando local", "Por favor, aguarde...", false, false);
        } else {
            dialog.dismiss();
        }
    }

    private void startLocationUpdates() {

        //Inicia GPS para buscar o local atual
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showAlertOk("Permissão para acessar GPS não foi concedida, portanto não será possível buscar o endereço atual automaticamente.");
        } else {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(2000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            showProgress(true);
        }

        if (!edtLatitude.getText().toString().isEmpty() || !edtLongitude.getText().toString().isEmpty())
            return;
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        showProgress(false);
    }

    private Address buscarEndereco(double latitude, double longitude) throws IOException {
        Address address = null;
        List<Address> addresses;

        Geocoder geocoder = new Geocoder(getApplicationContext());
        addresses = geocoder.getFromLocation(latitude, longitude, 1);
        if (addresses.size() > 0) {
            address = addresses.get(0);
        }

        return address;
    }

    private void limparCampos() {
        edtNome.setText("");
        edtLongitude.setText("");
        edtLatitude.setText("");
        edtEndereco.setText("");
        spnBebidas.setSelection(0);
    }

    private boolean campoObrigatorio() {
        final String nomeLocal = edtNome.getText().toString();
        final String logintudeLocal = edtLongitude.getText().toString();
        final String latitudeLocal = edtLatitude.getText().toString();
        final String enderecoLocal = edtEndereco.getText().toString();
        int charCount = 0;
        View focusView = null;

        for (int i = 0; i < enderecoLocal.length(); i++) {
            if (enderecoLocal.substring(i,i+1).equals(",")) {
                charCount++;
            }
        }
        if (charCount < 3 && !TextUtils.isEmpty(enderecoLocal))  {
            edtEndereco.setError("Endereço incompleto");
            focusView = edtEndereco;

            focusView.requestFocus();
            return true;
        }
        else if (TextUtils.isEmpty(nomeLocal) || TextUtils.isEmpty(logintudeLocal) || TextUtils.isEmpty(latitudeLocal)
                || TextUtils.isEmpty(enderecoLocal)) {

            if (TextUtils.isEmpty(nomeLocal)) {
                edtNome.setError("Campo Obrigatório");
                focusView = edtNome;
            } else if (TextUtils.isEmpty(enderecoLocal)) {
                edtEndereco.setError("Campo Obrigatório");
                focusView = edtEndereco;
            } else {
                edtLongitude.setError("Campo Obrigatório");
                edtLatitude.setError("Campo Obrigatório");
                focusView = edtLatitude;
            }

            focusView.requestFocus();
            return true;
        } else {

            focusView = edtNome;
            focusView.requestFocus();
            return false;
        }

    }

}
