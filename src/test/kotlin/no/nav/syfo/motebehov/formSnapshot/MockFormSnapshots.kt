package no.nav.syfo.motebehov.formSnapshot

val mockArbeidsgiverSvarOnskerSykmelderFormSnapshot = FormSnapshot(
    "motebehov-arbeidsgiver-svar",
    "1.0.0",
    listOf(
        RadioGroupFieldSnapshot(
            "harBehovRadioGroup",
            "Har dere behov for et dialogmøte med NAV?",
            "Du svarer på vegne av arbeidsgiver. Den ansatte har fått det samme spørsmålet og svarer på " +
                "vegne av seg selv.",
            "ja",
            "Ja, vi har behov for et dialogmøte.",
            listOf(
                FormSnapshotFieldOption("ja", "Ja, vi har behov for et dialogmøte.", true),
                FormSnapshotFieldOption("nei", "Nei, vi har ikke behov for et dialogmøte nå.")
            )
        ),
        TextFieldSnapshot(
            "begrunnelseText",
            "Begrunnelse (må fylles ut)",
            "Hva ønsker du å ta opp i møtet? Hva tenker du at NAV kan bistå med? Ikke skriv sensitiv " +
                "informasjon, for eksempel detaljerte opplysninger om helse.",
            "Vi trenger litt hjelp med videre tiltak",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerSykmelderDeltarCheckbox",
            "Jeg ønsker at sykmelder (lege/behandler) også deltar i møtet.",
            null,
            true
        ),
        TextFieldSnapshot(
            "onskerSykmelderDeltarBegrunnelseText",
            "Hvorfor ønsker du at lege/behandler deltar i møtet? (Må fylles ut)",
            null,
            "Ønsker å høre leges tanker rundt mulige tiltak for tilrettelegging",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerTolkCheckbox",
            "Vi har behov for tolk.",
            null,
            false
        )
    )
)

val mockArbeidstakerSvarFormSnapshot = FormSnapshot(
    "motebehov-arbeidstaker-svar",
    "1.0.0",
    listOf(
        RadioGroupFieldSnapshot(
            "harBehovRadioGroup",
            "Ønsker du et dialogmøte med NAV og arbeidsgiveren din?",
            null,
            "ja",
            "Ja, jeg ønsker et dialogmøte.",
            listOf(
                FormSnapshotFieldOption("ja", "Ja, jeg ønsker et dialogmøte.", true),
                FormSnapshotFieldOption("nei", "Nei, jeg mener det ikke er behov for et dialogmøte.")
            )
        ),
        TextFieldSnapshot(
            "begrunnelseText",
            "Begrunnelse (valgfri)",
            "Hva ønsker du å ta opp i møtet? Ikke skriv sensitiv informasjon, for eksempel detaljerte " +
                "opplysninger om helse.",
            "Ønsker å snakke om mine behov for tilrettelegging",
            wasOptional = true
        ),
        SingleCheckboxFieldSnapshot(
            "onskerSykmelderDeltarCheckbox",
            " Jeg ønsker at den som har sykmeldt meg (lege/behandler) også deltar i møtet.",
            null,
            false
        ),
        SingleCheckboxFieldSnapshot(
            "onskerTolkCheckbox",
            " Jeg har behov for tolk.",
            null,
            false
        )
    )
)

val mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot = FormSnapshot(
    "motebehov-arbeidsgiver-meld",
    "1.0.0",
    listOf(
        TextFieldSnapshot(
            "begrunnelseText",
            "Hvorfor ønsker du et dialogmøte? (Må fylles ut)",
            "Hva ønsker du å ta opp i møtet? Hva tenker du at NAV kan bistå med? Ikke skriv sensitiv " +
                "informasjon, for eksempel detaljerte opplysninger om helse.",
            "Vi trenger å ta en fot i bakken.",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerSykmelderDeltarCheckbox",
            "Jeg ønsker at sykmelder (lege/behandler) også deltar i møtet.",
            null,
            true
        ),
        TextFieldSnapshot(
            "onskerSykmelderDeltarBegrunnelseText",
            "Hvorfor ønsker du at lege/behandler deltar i møtet? (Må fylles ut)",
            null,
            "Ønsker å høre leges tanker rundt mulige tiltak for tilrettelegging",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerTolkCheckbox",
            "Vi har behov for tolk.",
            null,
            true
        ),
        TextFieldSnapshot(
            "tolkSprakText",
            "Hva slags tolk har dere behov for? (Må fylles ut)",
            "Oppgi for eksempel et språk eller tegnspråktolk.",
            "Tegnspråk",
        )
    )
)

val mockFormSnapshots = mapOf(
    "mockArbeidsgiverSvarOnskerSykmelderFormSnapshot" to mockArbeidsgiverSvarOnskerSykmelderFormSnapshot,
    "mockArbeidstakerSvarFormSnapshot" to mockArbeidstakerSvarFormSnapshot,
    "mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot" to mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot
)
