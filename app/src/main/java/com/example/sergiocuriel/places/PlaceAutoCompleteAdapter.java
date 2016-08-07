package com.example.sergiocuriel.places;

/**
 * Created by theserglan on 06/08/2016.
 */

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;


import android.content.Context;
import android.graphics.Typeface;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


public class PlaceAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable{

    private static final String TAG = "PlaceAutocompleteAdapter";
    private static final CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);/**Negritas*/
    /**
     * Resultados del adaptador.
     */
    private ArrayList<AutocompletePrediction> mResultList;

    /**
     * Maneja la respuesta del auto completado
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Los limites usadas por Places Geo Data
     * The bounds used for Places Geo Data autocomplete API requests.
     */
    private LatLngBounds mBounds;

    /**
     * El filtro de autocompletado suele restringis los queries a un tipo especifico de lugares
     */
    private AutocompleteFilter mPlaceFilter;

    /**
     *
     * Constructor del adaptador
     */
    public PlaceAutoCompleteAdapter(Context context, GoogleApiClient googleApiClient,
                                    LatLngBounds bounds, AutocompleteFilter filter) {
        super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        mGoogleApiClient = googleApiClient;
        mBounds = bounds;
        mPlaceFilter = filter;
    }

    /**
     * Setea los limites para los queries siguientes
     */
    public void setBounds(LatLngBounds bounds) {
        mBounds = bounds;
    }

    /**
     * Regresa el numero de resultados del autocompletado
     */
    @Override
    public int getCount() {
        return mResultList.size();
    }

    /**
     * Devuleve un item de la ultima query
     */
    @Override
    public AutocompletePrediction getItem(int position) {
        return mResultList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);


        /**
         *Setea el texto principal y secundario del lugar en 2 filas
         *  getPrimaryText() y getSecondaryText() regresa un CharSequence pero necesita que sea
         *  estilizado mediante CharacterStyle en el ejemplo use Negritas
         */

        AutocompletePrediction item = getItem(position);
        TextView textView1 = (TextView) row.findViewById(android.R.id.text1);
        TextView textView2 = (TextView) row.findViewById(android.R.id.text2);
        textView1.setText(item.getPrimaryText(STYLE_BOLD));
        textView2.setText(item.getSecondaryText(STYLE_BOLD));

        return row;
    }

    /**
     * Regresa el filtro de los resultados
     */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                // Skip the autocomplete query if no constraints are given.
                // Evita el autocompletado si no hay restricciones
                if (constraint != null) {
                    // Resultado segun la restriccion (busqueda).
                    mResultList = getAutocomplete(constraint);
                    if (mResultList != null) {
                        // La API encontro coicidencias
                        results.values = mResultList;
                        results.count = mResultList.size();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // La API trajo almenos un resultado.
                    notifyDataSetChanged();
                } else {
                    // La API no trajo ni un resultado
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                /**Se sobreescribe este metodo para mostrar un resultado en el AutocompleteTextView
                * cuando se da un clic sobre el
                */
                if (resultValue instanceof AutocompletePrediction) {
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                } else {
                    return super.convertResultToString(resultValue);
                }
            }
        };
    }

    /**
     * Envia un query a Places Geo Data AutoComplete API
     * Los resultados se devuelven como objetos de AutocompletePrediction,
     * los cuales contienen el Place ID y la descripcion del lugar
     * Regresa una lista vacia si no hay coincidencias
     * Regresa un null si la API no esta disponible no si hubo una falla en el Query
     * @param constraint Es la query escrita por el usuario
     */
    private ArrayList<AutocompletePrediction> getAutocomplete(CharSequence constraint) {
        if (mGoogleApiClient.isConnected()) {
            Toast.makeText(getContext(), "Iniciando autocompletado para : " + constraint, Toast.LENGTH_SHORT);

            // Peticion de autocompletado con la query (constraint)
            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi
                            .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                    mBounds, mPlaceFilter);

            // Este metodo hace una espera de 60s
            AutocompletePredictionBuffer autocompletePredictions = results
                    .await(60, TimeUnit.SECONDS);

            // Verifica si la peticion fue completada con exito y en caso de que no, regresa nulo
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                Toast.makeText(getContext(), "Error con la API: " + status.toString(),
                        Toast.LENGTH_SHORT).show();
                autocompletePredictions.release();
                return null;
            }

            // Congela los resultados para ser almacenados seguramente
            return DataBufferUtils.freezeAndClose(autocompletePredictions);
        }
        return null;
    }


}


