package no.nav.syfo.motebehov.formSnapshot

const val MOCK_ARRBEIDSGIVER_SVAR_BEGRUNNELSE = "Vi trenger litt hjelp med videre tiltak"
const val MOCK_ARBEIDSGIVER_SVAR_ONSKER_SYKMELDER_BEGRUNNELSE =
    "Ønsker å høre leges tanker rundt mulige tiltak for tilrettelegging"
const val MOCK_ARBEIDSGIVER_SVAR_SPRAK = "Tegnspråk"

const val MOCK_SNAPSHOTS_FORM_SEMANTIC_VERSION = "1.0.0"

val mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSGIVER_SVAR,
    MOCK_SNAPSHOTS_FORM_SEMANTIC_VERSION,
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
            MOCK_ARRBEIDSGIVER_SVAR_BEGRUNNELSE,
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
            MOCK_ARBEIDSGIVER_SVAR_ONSKER_SYKMELDER_BEGRUNNELSE,
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
            MOCK_ARBEIDSGIVER_SVAR_SPRAK,
        )
    )
)

val mockArbeidsgiverSvarNeiFormSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSGIVER_SVAR,
    MOCK_SNAPSHOTS_FORM_SEMANTIC_VERSION,
    listOf(
        RadioGroupFieldSnapshot(
            "harBehovRadioGroup",
            "Har dere behov for et dialogmøte med NAV?",
            "Du svarer på vegne av arbeidsgiver. Den ansatte har fått det samme spørsmålet og svarer på " +
                "vegne av seg selv.",
            "nei",
            "Nei, vi har ikke behov for et dialogmøte nå.",
            listOf(
                FormSnapshotFieldOption("ja", "Ja, vi har behov for et dialogmøte."),
                FormSnapshotFieldOption("nei", "Nei, vi har ikke behov for et dialogmøte nå.", true)
            )
        ),
        TextFieldSnapshot(
            "begrunnelseText",
            "Begrunnelse (må fylles ut)",
            "Hvorfor mener du det ikke er behov for et dialogmøte? Ikke skriv sensitiv informasjon, " +
                "for eksempel detaljerte opplysninger om helse.",
            "Vi har allerede god dialog",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerSykmelderDeltarCheckbox",
            "Jeg ønsker at sykmelder (lege/behandler) også deltar i møtet.",
            null,
            false
        ),
        SingleCheckboxFieldSnapshot(
            "onskerTolkCheckbox",
            "Vi har behov for tolk.",
            null,
            false
        )
    )
)

val mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSGIVER_MELD,
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

val mockArbeidstakerSvarJaFormSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSTAKER_SVAR,
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
            false
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
            true
        ),
        TextFieldSnapshot(
            "tolkSprakText",
            "Hva slags tolk har du behov for? (Må fylles ut)",
            "Oppgi for eksempel et språk eller tegnspråktolk.",
            "tegnspråk"
        )
    )
)

val mockArbeidstakerSvarNeiFormSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSTAKER_SVAR,
    "1.0.0",
    listOf(
        RadioGroupFieldSnapshot(
            "harBehovRadioGroup",
            "Ønsker du et dialogmøte med NAV og arbeidsgiveren din?",
            null,
            "nei",
            "Nei, jeg mener det ikke er behov for et dialogmøte.",
            listOf(
                FormSnapshotFieldOption("ja", "Ja, jeg ønsker et dialogmøte."),
                FormSnapshotFieldOption("nei", "Nei, jeg mener det ikke er behov for et dialogmøte.", true)
            )
        ),
        TextFieldSnapshot(
            "begrunnelseText",
            "Begrunnelse (må fylles ut)",
            "Hvorfor mener du det ikke er behov for et dialogmøte? Ikke skriv sensitiv informasjon, " +
                "for eksempel detaljerte opplysninger om helse.",
            "Jeg tror ikke det er behov for et dialogmøte.",
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
        ),
    )
)

val mockArbeidstakerMeldSnapshot = FormSnapshot(
    MotebehovFormIdentifier.ARBEIDSTAKER_MELD,
    "1.0.0",
    listOf(
        TextFieldSnapshot(
            "begrunnelseText",
            "Hvorfor ønsker du et dialogmøte? (Må fylles ut)",
            "Ikke skriv sensitiv informasjon, for eksempel detaljerte opplysninger om helse.",
            "Det er noen ting jeg vil ta opp med NAV",
        ),
        SingleCheckboxFieldSnapshot(
            "onskerSykmelderDeltarCheckbox",
            "Jeg ønsker at den som har sykmeldt meg (lege/behandler) også deltar i møtet.",
            null,
            false
        ),
        SingleCheckboxFieldSnapshot(
            "onskerTolkCheckbox",
            "Jeg har behov for tolk.",
            null,
            false
        )
    )
)

val mockFormSnapshots = mapOf(
    "mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot" to mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
    "mockArbeidsgiverSvarNeiFormSnapshot" to mockArbeidsgiverSvarNeiFormSnapshot,
    "mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot" to mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot,
    "mockArbeidstakerSvarJaFormSnapshot" to mockArbeidstakerSvarJaFormSnapshot,
    "mockArbeidstakerSvarNeiFormSnapshot" to mockArbeidstakerSvarNeiFormSnapshot,
    "mockArbeidstakerMeldSnapshot" to mockArbeidstakerMeldSnapshot
)
