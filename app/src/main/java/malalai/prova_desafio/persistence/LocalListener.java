package malalai.prova_desafio.persistence;

import android.support.annotation.Nullable;

/**
 * Created by Bob on 26/06/2016.
 * Classe utilizada para informar a view que a adição do local funcionou ou não,
 * para que a view possa enviar as devidas mensagens para o usuário (Observer)
 */
public interface LocalListener {
    //Quando local adicionado com sucesso irá chamar o método onSuccess
    void onSuccess(@Nullable LocalResponse localResponse);
    //Quando ocorrer alguma falha ao adicionar o local, será chamado o método onFailure
    void onFailure(Throwable t);
}
