package no.nav.syfo.motebehov.formSnapshot

const val MOCK_FORM_SNAPSHOT_JSON_ARBEIDSTAKER_SVAR = """
        {
          "formIdentifier": "motebehov-arbeidstaker-svar",
          "semanticVersion": "1.0.0",
          "snapshotFields": [
            {
              "fieldID": "harBehovRadioGroup",
              "fieldLabel": "Ønsker du et dialogmøte med NAV og arbeidsgiveren din?",
              "fieldType": "radioGroup",
              "selectedOptionId": "ja",
              "selectedOptionLabel": "Ja, jeg ønsker et dialogmøte.",
              "options": [
                {
                  "optionId": "ja",
                  "optionLabel": "Ja, jeg ønsker et dialogmøte.",
                  "wasSelected": true
                },
                {
                  "optionId": "nei",
                  "optionLabel": "Nei, jeg mener det ikke er behov for et dialogmøte.",
                  "wasSelected": false
                }
              ],
              "wasOptional": false
            },
            {
              "fieldID": "begrunnelse",
              "fieldLabel": "Begrunnelse (valgfri)",
              "description": "Hva ønsker du å ta opp i møtet? Ikke skriv sensitiv informasjon, for eksempel detaljerte opplysninger om helse.",
              "fieldType": "text",
              "textValue": "Vi må snakke om arbeidsoppgavene mine",
              "wasOptional": true
            },
            {
              "fieldId": "onskerSykmelderDeltar",
              "fieldLabel": "Jeg ønsker at den som har sykmeldt meg (lege/behandler) også deltar i møtet.",
              "fieldType": "checkboxSingle",
              "wasChecked": true
            },
            {
              "fieldID": "onskerSykmelderDeltarBegrunnelse",
              "fieldLabel": "Hvorfor ønsker du at lege/behandler deltar i møtet? (Må fylles ut)",
              "fieldType": "text",
              "textValue": "Vil snakke med legen om noen ting",
              "wasOptional": false
            },
            {
              "fieldId": "onskerTolk",
              "fieldLabel": "Jeg har behov for tolk.",
              "fieldType": "checkboxSingle",
              "wasChecked": false
            }
          ]
        }
    """
