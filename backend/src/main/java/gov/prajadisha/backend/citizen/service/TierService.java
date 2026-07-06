package gov.prajadisha.backend.citizen.service;

import org.springframework.stereotype.Service;

/**
 * Maps a citizen's wallet balance to a gamified tier.
 */
@Service
public class TierService {

    public String tierFor(int points) {
        if (points >= 2500) return "Gold Citizen";
        if (points >= 1000) return "Silver Citizen";
        return "Bronze Citizen";
    }
}
