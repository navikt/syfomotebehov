UPDATE motebehov_form_values
SET form_snapshot = (
    SELECT jsonb_set(
                   form_snapshot,
                   '{fieldSnapshots}',
                   (
                       SELECT jsonb_agg(
                                      CASE
                                          WHEN elem->>'fieldId' = 'begrunnelseText'
                                              THEN elem || '{"value": ""}'::jsonb
                                          ELSE elem
                                          END
                              )
                       FROM jsonb_array_elements(form_snapshot->'fieldSnapshots') AS elem
                   )
           )
),
    begrunnelse = NULL
WHERE motebehov_row_id = 1486553;
