package no.nav.doksikkerhetsnett.service;

import static org.apache.tomcat.jni.Time.sleep;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FinnOppgaveService {
	private final String AAPEN = "AAPEN";
	private final String AVSLUTTET = "AVSLUTTET";
	private final String DEFAULT_URL_P1 = "&sorteringsrekkefolge=ASC&";
	private final String DEFAULT_URL_P2 = "&limit=20";

	FinnOppgaveConsumer finnOppgaveConsumer;

	@Inject
	public FinnOppgaveService(FinnOppgaveConsumer finnOppgaveConsumer) {
		this.finnOppgaveConsumer = finnOppgaveConsumer;
	}

	public ArrayList<FinnOppgaveResponse> finnOppgaver(List<UbehandletJournalpost> ubehandledeJournalposter, boolean checkForOpenJournalPosts) {
		ArrayList<String> idStrings = Utils.journalpostListToJournalpostIdList(ubehandledeJournalposter);
		String status = getStatus(checkForOpenJournalPosts);
		ArrayList<FinnOppgaveResponse> oppgaveResponses = new ArrayList<>();

		for (String journalpostIdsAsString : idStrings) {
			FinnOppgaveResponse oppgaveResponse =
				finnOppgaveConsumer.finnOppgaveForJournalposter(journalpostIdsAsString + "&statuskategori=" + status + DEFAULT_URL_P1 + DEFAULT_URL_P2);
			oppgaveResponses.add(oppgaveResponse);
			int differenceBetweenTotalReponsesAndResponseList = oppgaveResponse.getAntallTreffTotalt() - oppgaveResponse.getOppgaver()
					.size();
			if (differenceBetweenTotalReponsesAndResponseList != 0) {
				int extraPages = differenceBetweenTotalReponsesAndResponseList / 20;

				for (int i = 1; i <= extraPages + 1; i++) {
					oppgaveResponses.add(
							finnOppgaveConsumer.finnOppgaveForJournalposter(
									journalpostIdsAsString + "&statuskategori=" + status + DEFAULT_URL_P1 + "offset=" + i * 20 + DEFAULT_URL_P2));
				}
			}
		}
		return oppgaveResponses;
	}

	public String getStatus(boolean checkForOpenJournalPosts) {
		if (checkForOpenJournalPosts) {
			return AAPEN;
		} else {
			return AVSLUTTET;
		}
	}
}
