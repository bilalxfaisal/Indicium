package com.indicium.repository;
import com.indicium.models.Evidence;
import java.util.ArrayList;
import java.util.*;

public class EvidenceRepo
{
    private static Map<Integer, List<Evidence>> caseEvidenceMap;
    private static List<Evidence> evidenceList;

    public EvidenceRepo()
    {
        caseEvidenceMap = null;
        evidenceList = null;
    }

    public static void add(Evidence evidence, int caseID)
    {
        caseEvidenceMap.putIfAbsent(caseID, new ArrayList<>());
        caseEvidenceMap.get(caseID).add(evidence);
        if (evidenceList == null) evidenceList = new ArrayList<>();
        evidenceList.add(evidence.getEvidenceID(), evidence);
    }

    public static List<Evidence> findByCase(int caseID)
    {
        List<Evidence> list;
        list = caseEvidenceMap.get(caseID);
        return list;
    }

    public static Evidence getEvidence(int evidenceID)
    {
        Evidence evidence;
        evidence = evidenceList.get(evidenceID);
        return evidence;
    }
}
