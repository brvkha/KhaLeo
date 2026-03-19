Draft PR: Limit ASG footprint + RDS sizing proposal

Summary
- Reduce EC2 autoscaling defaults to limit cost exposure: `backend_min_size=1`, `backend_desired_capacity=1`, `backend_max_size=4`.
- Add an RDS/Aurora sizing proposal recommending `db.t4g.small` as a conservative next step and describing options (serverless, single RDS, backup retention, IOPS).

Files changed
- `infra/terraform/variables.tf` (update ASG defaults)
- `infra/terraform/proposals/rds-scaling-proposal.md` (new)

Testing notes
1. Create a feature branch and push changes.
2. Run `terraform init` and `terraform plan` in your staging workspace (use appropriate `backend` and `secrets.tfvars`). Review planned changes — changing ASG defaults should be safe and non-destructive.
3. If you want to resize RDS in this PR, run `terraform plan` and review estimated replacement/resizing. Prefer doing RDS resize in a separate controlled change and staging testing.

Commands (example)

```bash
# create branch
git checkout -b feat/cost-limit-asg

# stage changes
git add infra/terraform/variables.tf infra/terraform/proposals/rds-scaling-proposal.md

# commit
git commit -m "Limit ASG defaults (min=1,desired=1,max=4) + add RDS sizing proposal"

# push
git push origin feat/cost-limit-asg

# open draft PR using GitHub CLI
gh pr create --title "feat: limit ASG defaults + RDS sizing proposal" \
  --body-file infra/terraform/pr-draft-cost-limit-asg-and-rds.md --draft
```

Checklist
- [ ] Run `terraform plan` in staging and validate no unintended replacements
- [ ] Run load tests against staging with 1 instance to validate performance
- [ ] If acceptable, merge and monitor production costs/metrics
- [ ] (Optional) Follow-up PR to change `aurora_instance_class` in staging and test

Notes
- I did not change any RDS values automatically; `rds-scaling-proposal.md` lists options and next steps for RDS resizing.
- If you want, I can create the feature branch and open a draft PR for you — but I need git remote push access from this environment or you can run the above commands locally.
