RDS / Aurora Sizing & Cost-Saving Proposal

Goal
- Reduce ongoing Aurora costs while balancing availability and performance.

Current (from variables.tf)
- `aurora_instance_class`: db.t4g.medium
- `aurora_instance_count`: 1

Quick options (ordered by cost savings / risk):

1) Conservative: smaller instance class
- Change `aurora_instance_class` from `db.t4g.medium` -> `db.t4g.small` (or `db.t4g.micro` for very light load).
- Minimal risk; keeps single AZ HA characteristics of Aurora but smaller vCPU/memory.
- Terraform change example:

```hcl
variable "aurora_instance_class" {
  type    = string
  default = "db.t4g.small"
}
```

2) Moderate: reduce replica count (if >1)
- If you currently have multiple reader instances, reduce `aurora_instance_count` to 1.
- Risk: no read-scaling / reduced HA for readers.

3) Aggressive: Aurora Serverless v2
- Migrate to Aurora Serverless v2 to pay for consumed capacity rather than always-on instances.
- Best for variable/low traffic; migration effort required and some features differ.

4) Alternative: single RDS (MySQL) instance
- If HA is not required, single RDS instance is cheaper than Aurora clusters.
- Risk: no cluster-level HA and fewer Aurora features.

Other cost levers
- Reduce `backup_retention_period` (e.g., 7 -> 3 days) if acceptable.
- Turn off `copy_tags_to_snapshot` / skip final snapshot on destroy (already true).
- Check Provisioned IOPS vs gp3; move to gp3 if possible.

Recommended next steps
1. Run production telemetry: collect CPU/RPS/DB-connections over 7–14 days.
2. Try changing `aurora_instance_class` to `db.t4g.small` in a staging environment and run load tests.
3. If load is low, propose changing `aurora_instance_class` in Terraform and open a PR.
4. Consider Aurora Serverless v2 for variable workloads — plan migration separately.

Terraform action items (draft PR)
- Update `variables.tf` default for `aurora_instance_class`.
- Add a short `README` note documenting the tradeoffs.
- Run `terraform plan` and review estimated changes (no destructive step expected when only resizing class).

If you want, I will:
- Create a PR draft that changes `aurora_instance_class` to `db.t4g.small` and includes testing notes.
- Or I can open a branch and leave the change as a configurable variable for you to review.


