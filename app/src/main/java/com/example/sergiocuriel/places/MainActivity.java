package com.example.sergiocuriel.places;

import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**Elemento de conexion con GoogleAPIClient*/
    private GoogleApiClient mGoogleApiClient;
    /**Adaptador*/
    private PlaceAutoCompleteAdapter mAdapter;
    /**Buscador*/
    private AutoCompleteTextView mAutocompleteView;
    /**Detalles*/
    private TextView mPlaceDetailsText;

    private TextView mPlaceDetailsAttribution;

    /**Limites en coordenadas, new LatLngBounds(new LatLng(coordenadas del sudoeste), new LatLng(coordenadas del noreste)); */
    private static final LatLngBounds BOUNDS_EJEMPLO = new LatLngBounds(
            new LatLng(	0.00, -0.00), new LatLng(0.00, -0.00));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)//Constructor
                .addApi(Places.GEO_DATA_API)//Se agrega la api
                .addConnectionCallbacks(this)//Callbacks
                .addOnConnectionFailedListener(this)//Listener de errores de conexion
                .build();

        /** Buscador autocompletado*/
        mAutocompleteView = (AutoCompleteTextView)
                findViewById(R.id.autocomplete_places);

        /** Listener para cuando se da click en una sugerencia*/
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);

        /** TextViews con info del sitio elegido*/
        mPlaceDetailsText = (TextView) findViewById(R.id.place_details);
        mPlaceDetailsAttribution = (TextView) findViewById(R.id.place_attribution);

        /**Este adaptador recibe las sugerencias de la API
         * El constructor recibe: (contexto, GoogleApiClient, Limites, Filtro)*/
        mAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient, BOUNDS_EJEMPLO, null);
        /**Se asigna el adaptador a la view del autocompletado*/
        mAutocompleteView.setAdapter(mAdapter);

        /**Botón para limpiar el buscador*/
        Button clearButton = (Button) findViewById(R.id.button_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAutocompleteView.setText("");
            }
        });

    }

    /**Listener para los clics en las sugerencias del autocompletado*/
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /**
             Devuele el item seleccionado y guarda el ID del lugar en la variable placeId
             El adaptador almacena cada sugerencia de las Predicciones de donde se toma el titulo e ID
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            /**
            Hace una peticion que trae informacion adicional sobre los detalles del lugar
              */

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getApplicationContext(), "Clicked: " + primaryText,
                    Toast.LENGTH_SHORT).show();

        }
    };

    /**
     * Resultados de Places Geo Data API
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            /**Resultados en caso de falla*/
            if (!places.getStatus().isSuccess()) {
                Log.e("TAG", "Error en el query. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            /**Regresa el lugar.*/
            final Place place = places.get(0);

            /** Da formato a los detalles del lugar*/
            mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
                    place.getId(), place.getAddress(), place.getPhoneNumber(),
                    place.getWebsiteUri()));

            /**
             *El siguiente codigo permite trae contribuciones de terceros sobre el lugar*/
            final CharSequence thirdPartyAttribution = places.getAttributions();

            /**Oculta o muestra segun haya o no atribuciones de terceros*/
            if (thirdPartyAttribution == null) {
                mPlaceDetailsAttribution.setVisibility(View.GONE);
            } else {
                mPlaceDetailsAttribution.setVisibility(View.VISIBLE);
                /**Se le da formato desde un html alojado en strings*/
                mPlaceDetailsAttribution.setText(Html.fromHtml(thirdPartyAttribution.toString()));
            }

            Log.i("TAG", "Detalles recibidos de: " + place.getName());

            places.release();
        }
    };

    private static Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                              CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
        Log.e("TAG", res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));
        /**Se le da formato a los detalles desde un html en strings*/
        return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));

    }

    /**
     * Cuando no se puede conectar con GooglePlayServices el auto-manejador resuelve el problema y
     * @param connectionResult determina la causa del fallo
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /**error en consola o toast*/
        Log.e("TAG", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        /**Conecta con GooglePlayServices*/
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        /**Desconecta a GooglePlayServices*/
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "CONEXION EXITOSA", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"CONEXION SUSPENDIDA",Toast.LENGTH_SHORT).show();
    }

}
